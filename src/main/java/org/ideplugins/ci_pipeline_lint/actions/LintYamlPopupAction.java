package org.ideplugins.ci_pipeline_lint.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import static org.ideplugins.ci_pipeline_lint.actions.ActionHelper.displayNotificationWithAction;


public class LintYamlPopupAction extends BaseAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(psiFile != null && GITLAB_CI_YML.equals(psiFile.getName()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null) {
            Project project = event.getProject();
            if (project == null) {
                displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab Host, Token and Project ID");
                return;
            }
            if (areHostAndTokenMissing(project)) {
                displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab Host and Token");
            } else if (isProjectIdMissing(project)) {
                // prompt user to enter project id when host and token are configured
                promptForProjectId(project);
            } else {
                doLintInBackground(event, psiFile);
            }
        } else {
            displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab Host, Token and Project ID");
        }
    }


}

