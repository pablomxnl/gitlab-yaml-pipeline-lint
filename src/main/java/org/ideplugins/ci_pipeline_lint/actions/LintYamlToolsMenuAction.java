package org.ideplugins.ci_pipeline_lint.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.ideplugins.ci_pipeline_lint.settings.ProjectGitSettingsState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.ideplugins.ci_pipeline_lint.actions.ActionHelper.*;


public class LintYamlToolsMenuAction extends BaseAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }


    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = getEventProject(event);
        if (project == null) {
            displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab Host, Token and Project ID");
            return;
        }

        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        List<VirtualFile> files = new ArrayList<>(FilenameIndex.getVirtualFilesByName(GITLAB_CI_YML, scope));
        if (files.isEmpty()) {
            displayNotification(NotificationType.WARNING, "No .gitlab-ci.yml file found.");
            return;
        }
        if (files.size() > 1) {
            displayNotification(NotificationType.WARNING,
                    "Multiple .gitlab-ci.yml files found on project. Currently not supported");
            return;
        }

        if (areHostAndTokenMissing(project)) {
            displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab Host and Token");
            return;
        }

        if (isProjectIdMissing(project)) {
            displayNotificationWithAction(NotificationType.WARNING,
                    "Couldn't determine Project ID",
                    "Please setup your Project ID",
                    ()->{
                        String input = Messages.showInputDialog(project,
                                "Could not detect GitLab project ID automatically.\nPlease enter Project ID:",
                                "Enter Project ID",
                                null);
                        if (input != null && !input.isBlank()) {
                            ProjectGitSettingsState projectState = project.getService(ProjectGitSettingsState.class);
                            projectState.projectId = input.trim();

                            doLintPipelineFile(event, project, files);
                        }

                    }
            );
            return;
        }

        doLintPipelineFile(event, project, files);
    }

    private void doLintPipelineFile(@NotNull AnActionEvent event, Project project, List<VirtualFile> files) {
        DumbService dumbService = DumbService.getInstance(Objects.requireNonNull(project));
        if (dumbService.isDumb()){
            dumbService.runWhenSmart(() -> lintPipelineFile(project, files, event));
        } else {
            lintPipelineFile(project, files, event);
        }
    }

    private void lintPipelineFile(@NotNull Project project, List<VirtualFile> files,  @NotNull AnActionEvent event) {
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(files.getFirst());
        if (psiFile != null) {
            doLintInBackground(event, psiFile);
        }
    }

}
