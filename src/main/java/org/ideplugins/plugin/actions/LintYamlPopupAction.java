package org.ideplugins.plugin.actions;

import com.google.gson.JsonObject;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import org.ideplugins.plugin.linter.Constants;
import org.ideplugins.plugin.linter.YamlPipelineLinter;
import org.jetbrains.annotations.NotNull;

import static org.ideplugins.plugin.actions.ActionHelper.*;

public class LintYamlPopupAction extends AnAction implements Constants {
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(psiFile != null && GITLAB_CI_YML.equals(psiFile.getName()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (checkGitlabToken()) {
            PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
            if (psiFile != null) {
                JsonObject yamlJson = ActionHelper.getYamlJson(psiFile);
                ApplicationManager.getApplication().invokeLater(() -> {
                    YamlPipelineLinter linter =
                            new YamlPipelineLinter(ActionHelper.getGitlabUrl(), ActionHelper.getGitlabToken());
                    showLintResult(linter.ciLint(yamlJson), event);
                });
            }
        } else {
            displayNotificationWihAction(NotificationType.WARNING, "Please setup your Gitlab token");
        }
    }
}
