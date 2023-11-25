package org.ideplugins.gitlab_pipeline_lint.actions;

import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.ideplugins.gitlab_pipeline_lint.linter.YamlPipelineLinter;
import org.ideplugins.gitlab_pipeline_lint.service.PipelineIssuesReporter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper.*;

public abstract class BaseAction extends AnAction implements Constants {
    @RequiresBackgroundThread
    protected void doLintInBackground(@NotNull AnActionEvent event, @NotNull PsiFile psiFile) {
        String gitlabCILintEndpoint = getGitlabUrl();
        JsonObject yamlJson = ActionHelper.getYamlJson(psiFile);
        YamlPipelineLinter linter =
                new YamlPipelineLinter(gitlabCILintEndpoint, getGitlabToken());
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
