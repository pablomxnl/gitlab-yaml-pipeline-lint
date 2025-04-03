package org.ideplugins.ci_pipeline_lint.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;


public class FeedbackToolwindowToolbarAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        FeedbackActionHelper feedbackActionHelper = new FeedbackActionHelper(actionEvent);
        String url = feedbackActionHelper.getEncodedUrl();
        BrowserUtil.browse(url);
    }


}
