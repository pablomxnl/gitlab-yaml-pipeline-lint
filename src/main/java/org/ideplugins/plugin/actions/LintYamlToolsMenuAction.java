package org.ideplugins.plugin.actions;

import com.google.gson.JsonObject;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.ideplugins.plugin.linter.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.ideplugins.plugin.actions.ActionHelper.*;
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
            List<VirtualFile> files  = new ArrayList<>(FilenameIndex.getVirtualFilesByName(GITLAB_CI_YML, scope));

            if (files.size() == 0) {
                displayNotification(NotificationType.WARNING, "No .gitlab-ci.yml file found.");
                return;
            }

            if (files.size() > 1) {
                displayNotification(NotificationType.WARNING,
                        "Multiple .gitlab-ci.yml files found on project. Currently not supported");
                return;
            }

            PsiManager psiManager = PsiManager.getInstance(event.getProject());
            JsonObject yamlJson = ActionHelper.getYamlJson(Objects.requireNonNull(psiManager.findFile(files.get(0))));
            ApplicationManager.getApplication().invokeLater(() -> {
                JsonObject result = ciLint(yamlJson);
                showLintResult(result, event);
            });
        } else {
            displayNotificationWihAction(NotificationType.WARNING, "Please setup your Gitlab token");
        }
    }

}
