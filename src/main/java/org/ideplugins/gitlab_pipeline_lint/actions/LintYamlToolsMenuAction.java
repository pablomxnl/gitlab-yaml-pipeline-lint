package org.ideplugins.gitlab_pipeline_lint.actions;

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
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.ideplugins.gitlab_pipeline_lint.linter.YamlPipelineLinter;
import org.ideplugins.gitlab_pipeline_lint.service.PipelineIssuesReporter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper.*;

public class LintYamlToolsMenuAction extends AnAction implements Constants {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (checkGitlabToken()) {
            Optional.ofNullable(event.getProject()).ifPresent(project -> {
                GlobalSearchScope scope = GlobalSearchScope.projectScope(Objects.requireNonNull(event.getProject()));
                List<VirtualFile> files = new ArrayList<>(FilenameIndex.getVirtualFilesByName(GITLAB_CI_YML, scope));

                if (files.size() == 0) {
                    displayNotification(NotificationType.WARNING, "No .gitlab-ci.yml file found.");
                    return;
                }
                if (files.size() > 1) {
                    displayNotification(NotificationType.WARNING,
                            "Multiple .gitlab-ci.yml files found on project. Currently not supported");
                    return;
                }
                PsiManager psiManager = PsiManager.getInstance(project);
                Optional.ofNullable(psiManager.findFile(files.get(0))).ifPresent(psiFile -> {
                    JsonObject yamlJson = ActionHelper.getYamlJson(psiFile);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        YamlPipelineLinter linter =
                                new YamlPipelineLinter(ActionHelper.getGitlabUrl(), ActionHelper.getGitlabToken());
                        JsonObject gitlabResponse = linter.ciLint(yamlJson);
                        showLintResult(gitlabResponse, event);
                        PipelineIssuesReporter reporter = project.getService(PipelineIssuesReporter.class);
                        reporter.populateIssues(Map.of(files.get(0).getPath(), List.of(gitlabResponse)));
                    });
                });

            });
        } else {
            displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab token");
        }
    }

}
