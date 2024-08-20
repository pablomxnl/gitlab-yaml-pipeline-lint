package org.ideplugins.gitlab_pipeline_lint.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper.*;


public class LintYamlToolsMenuAction extends BaseAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }


    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        String gitlabCILintEndpoint = getGitlabUrl();
        if (checkGitlabToken() && !gitlabCILintEndpoint.contains("%") && event.getProject() != null) {
            Project project = getEventProject(event);
            DumbService dumbService = DumbService.getInstance(Objects.requireNonNull(project));
            if (dumbService.isDumb()){
                dumbService.runWhenSmart(() -> lintPipelineFile(project,event));
            } else {
                lintPipelineFile(project, event);
            }
        } else {
            displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab token/Project ID");
        }
    }

    private void lintPipelineFile(@NotNull Project project, @NotNull AnActionEvent event) {
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
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(files.getFirst());
        if (psiFile != null) {
            doLintInBackground(event, psiFile);
        }
    }

}
