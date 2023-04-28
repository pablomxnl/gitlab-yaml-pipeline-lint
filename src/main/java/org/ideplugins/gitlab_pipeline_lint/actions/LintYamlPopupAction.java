package org.ideplugins.gitlab_pipeline_lint.actions;

import com.google.gson.JsonObject;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtil;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.ideplugins.gitlab_pipeline_lint.linter.YamlPipelineLinter;
import org.ideplugins.gitlab_pipeline_lint.service.PipelineIssuesReporter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper.*;

public class LintYamlPopupAction extends AnAction implements Constants {
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(psiFile != null && GITLAB_CI_YML.equals(psiFile.getName()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        String gitlabCILintEndpoint = getGitlabUrl();
        if (checkGitlabToken() && !gitlabCILintEndpoint.contains("%")) {
            Optional.ofNullable(event.getProject()).ifPresent(project -> {
                PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
                if (psiFile != null) {
                    JsonObject yamlJson = ActionHelper.getYamlJson(psiFile);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        YamlPipelineLinter linter =
                                new YamlPipelineLinter(gitlabCILintEndpoint, ActionHelper.getGitlabToken());
                        JsonObject gitlabResponse = linter.ciLint(yamlJson);
                        showLintResult(gitlabResponse, event);

                        PipelineIssuesReporter reporter = project.getService(PipelineIssuesReporter.class);
                        reporter.populateIssues(Map.of(psiFile.getVirtualFile().getPath(), List.of(gitlabResponse)));
                        ApplicationManager.getApplication().invokeAndWait(FileContentUtil::reparseOpenedFiles);
                    });
                }
            });
        } else {
            displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab token/Project ID");
        }
    }
}
