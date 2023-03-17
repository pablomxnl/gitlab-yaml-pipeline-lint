package org.ideplugins.plugin.errorhandling;

import com.intellij.diagnostic.AbstractMessage;
import com.intellij.diagnostic.IdeaReportingEvent;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.Consumer;
import io.sentry.Sentry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class SentryErrorReporter extends ErrorReportSubmitter {

    public @NlsActions.ActionText @NotNull String getReportActionText() {
        return "Report to Plugin Author";
    }

    @Override
    public @NlsContexts.DetailedDescription @Nullable String getPrivacyNoticeText() {
        String path = getClass().getPackageName().replaceAll("\\.", "/");
        String fileName = getClass().getSimpleName();
        String url = "https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/blob/main/src/main/java/%s/%s.java";
        String gitlabSourceLink =
                String.format(url, path, fileName);
        String message =
                "This error reporter doesn't log any PII or system information, just the stacktrace, \n" +
                        String.format("For more information review the code <a href='%s'>hier</a> ", gitlabSourceLink);
        return message;
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo,
                          @NotNull Component parentComponent,
                          @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        DataContext context = DataManager.getInstance().getDataContext(parentComponent);
        Project project = CommonDataKeys.PROJECT.getData(context);

        new Task.Backgroundable(project, "Sending Error Report") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    submitErrors(events, additionalInfo);
                    Messages.showInfoMessage(parentComponent,
                            "Thank you for submitting your report!", "Error Report");
                    consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
                });
            }
        }.queue();

        return true;
    }

    private static void submitErrors(IdeaLoggingEvent @NotNull [] events, String additionalInfo) {
        for (IdeaLoggingEvent ideaEvent : events) {
            if (ideaEvent instanceof IdeaReportingEvent && ideaEvent.getData() instanceof AbstractMessage) {
                Throwable ex = ((AbstractMessage) ideaEvent.getData()).getThrowable();
                Sentry.withScope(scope ->{
                    scope.setExtra("userMessage", additionalInfo);
                    Sentry.captureException(ex);
                });
            }
        }
    }

}
