package org.ideplugins.gitlab_pipeline_lint.actions;

import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.ideplugins.gitlab_pipeline_lint.linter.YamlPipelineLinter;
import org.ideplugins.gitlab_pipeline_lint.service.PasswordSafeService;
import org.ideplugins.gitlab_pipeline_lint.service.PipelineIssuesReporter;
import org.ideplugins.gitlab_pipeline_lint.settings.YamlPipelineLintSettingsState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper.*;

public abstract class BaseAction extends AnAction implements Constants {

    protected boolean checkGitlabToken() {
        String token = PasswordSafeService.retrieveToken();
        return token != null && !token.isBlank();
    }

    protected boolean checkPluginSettings(Project project) {
        String gitlabCILintEndpoint = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class)
                .gitlabEndpoint;
        return checkGitlabToken() && !gitlabCILintEndpoint.contains("%");
    }

    @RequiresBackgroundThread
    protected void doLintInBackground(@NotNull AnActionEvent event, @NotNull PsiFile psiFile) {
        String gitlabCILintEndpoint = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class)
                .gitlabEndpoint;
        JsonObject yamlJson = ActionHelper.getYamlJson(psiFile);
        YamlPipelineLinter linter =
                new YamlPipelineLinter(gitlabCILintEndpoint, PasswordSafeService.retrieveToken());
        JsonObject gitlabResponse = linter.ciLint(yamlJson);
        showLintResult(gitlabResponse, event);

        PipelineIssuesReporter reporter =
                Objects.requireNonNull(getEventProject(event)).getService(PipelineIssuesReporter.class);
        reporter.populateIssues(Map.of(psiFile.getVirtualFile().getPath(), List.of(gitlabResponse)));
        FileContentUtil.reparseFiles(Objects.requireNonNull(getEventProject(event)),
                List.of(psiFile.getVirtualFile()), true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
