package org.ideplugins.ci_pipeline_lint.actions;

import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.ideplugins.ci_pipeline_lint.linter.Constants;
import org.ideplugins.ci_pipeline_lint.linter.YamlPipelineLinter;
import org.ideplugins.ci_pipeline_lint.service.PasswordSafeService;
import org.ideplugins.ci_pipeline_lint.service.PipelineIssuesReporter;
import org.ideplugins.ci_pipeline_lint.settings.YamlPipelineLintSettingsState;
import org.ideplugins.ci_pipeline_lint.settings.ProjectGitSettingsState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.ideplugins.ci_pipeline_lint.actions.ActionHelper.*;

public abstract class BaseAction extends AnAction implements Constants {

    protected boolean checkGitlabToken() {
        String token = PasswordSafeService.retrieveToken();
        return !token.isBlank();
    }

    protected boolean areHostAndTokenMissing(Project project) {
        var yamlState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        if (yamlState == null) return true;
        String host = yamlState.gitlabHost;
        return host == null || host.isBlank() || !checkGitlabToken();
    }

    protected boolean isProjectIdMissing(Project project) {
        ProjectGitSettingsState projectState = project == null ? null : project.getService(ProjectGitSettingsState.class);
        String projectId = projectState != null ? projectState.projectId : "";
        return projectId == null || projectId.isBlank();
    }

    protected void promptForProjectId(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            String input = Messages.showInputDialog(project,
                    "Could not detect GitLab project ID automatically.\nPlease enter Project ID:",
                    "Enter Project ID",
                    null);
            if (input != null && !input.isBlank()) {
                ProjectGitSettingsState projectState = project.getService(ProjectGitSettingsState.class);
                projectState.projectId = input.trim();
            }
        });
    }

    @RequiresBackgroundThread
    protected void doLintInBackground(@NotNull AnActionEvent event, @NotNull PsiFile psiFile) {
        JsonObject yamlJson = ActionHelper.getYamlJson(psiFile);
        Project project = Objects.requireNonNull(getEventProject(event));
        YamlPipelineLinter linter = new YamlPipelineLinter(project);
        AtomicReference<JsonObject> reference = new AtomicReference<>();
        try {
            ApplicationManager.getApplication().executeOnPooledThread(()-> {
                reference.getAndSet(linter.ciLint(yamlJson));
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        showLintResult(reference.get(), event);
        PipelineIssuesReporter reporter =
                Objects.requireNonNull(getEventProject(event)).getService(PipelineIssuesReporter.class);
        reporter.populateIssues(Map.of(psiFile.getVirtualFile().getPath(), List.of(reference.get())));

        FileContentUtil.reparseFiles(Objects.requireNonNull(getEventProject(event)),
                List.of(psiFile.getVirtualFile()), true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
