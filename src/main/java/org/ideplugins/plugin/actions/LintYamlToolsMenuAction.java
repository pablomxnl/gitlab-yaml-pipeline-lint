package org.ideplugins.plugin.actions;

import com.google.gson.JsonObject;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.ideplugins.plugin.linter.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.ideplugins.plugin.actions.ActionHelper.displayNotification;
import static org.ideplugins.plugin.actions.ActionHelper.showLintResult;
import static org.ideplugins.plugin.linter.YamlPipelineLinter.checkGitlabToken;
import static org.ideplugins.plugin.linter.YamlPipelineLinter.ciLint;

public class LintYamlToolsMenuAction extends AnAction implements Constants {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (checkGitlabToken()) {
            GlobalSearchScope scope = GlobalSearchScope.projectScope(Objects.requireNonNull(event.getProject()));
            PsiFile[] files =
                    FilenameIndex.getFilesByName(event.getProject(), GITLAB_CI_YML, scope);

            if (files.length == 0) {
                displayNotification(NotificationType.WARNING, "No .gitlab-ci.yml file found.");
                return;
            }

            if (files.length > 1) {
                displayNotification(NotificationType.WARNING,
                        "Multiple .gitlab-ci.yml files found on project. Currently not supported");
                return;
            }

            JsonObject yamlJson = ActionHelper.getYamlJson(files[0]);
            ApplicationManager.getApplication().invokeLater(() -> {
                JsonObject result = ciLint(yamlJson);
                showLintResult(result, event);
            });
        }
    }

}
