package org.ideplugins.gitlab_pipeline_lint.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;


import static org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper.*;


public class LintYamlPopupAction extends BaseAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(psiFile != null && GITLAB_CI_YML.equals(psiFile.getName()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (checkPluginSettings(event.getProject())) {
            PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
            doLintInBackground(event, psiFile);
        } else {
            displayNotificationWithAction(NotificationType.WARNING, "Please setup your Gitlab token/Project ID");
        }
    }


}

