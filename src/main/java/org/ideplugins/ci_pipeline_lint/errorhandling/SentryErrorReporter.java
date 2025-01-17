package org.ideplugins.ci_pipeline_lint.errorhandling;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.Consumer;
import io.sentry.Hub;
import io.sentry.IHub;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import org.ideplugins.ci_pipeline_lint.settings.PipelinePluginConfigurationState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;


public class SentryErrorReporter extends ErrorReportSubmitter {

    private static void submitErrors(IdeaLoggingEvent[] events, String additionalInfo, IHub sentryHub) {
        for (IdeaLoggingEvent ideaEvent : events) {
                sentryHub.setExtra("userMessage", additionalInfo);
                sentryHub.captureMessage(ideaEvent.getThrowableText(), SentryLevel.ERROR);
        }
    }

    public @NlsActions.ActionText @NotNull String getReportActionText() {
        return "Report to Plugin Author";
    }

    @Override
    public @Nullable String getPrivacyNoticeText() {
        String path = getClass().getPackageName().replaceAll("\\.", "/");
        String fileName = getClass().getSimpleName();
        String url = "https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/blob/main/src/main/java/%s/%s.java";
        String gitlabSourceLink =
                String.format(url, path, fileName);
        return "This error reporter doesn't log any PII or system information, just the stacktrace, \n" +
                String.format("For more information review the code <a href='%s'>here</a> ", gitlabSourceLink);
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events,
                          @Nullable String additionalInfo, @NotNull Component parentComponent,
                          @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        DataContext context = DataManager.getInstance().getDataContext(parentComponent);
        Project project = CommonDataKeys.PROJECT.getData(context);
        IHub sentryHub = initSentry();
        new Task.Backgroundable(project, "Sending error report") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    submitErrors(events, additionalInfo, sentryHub);
                    Messages.showInfoMessage(parentComponent,
                            "Thank you for submitting your report!", "Error Report");
                    consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
                });
            }
        }.queue();

        return true;
    }

    private IHub initSentry() {
        PluginDescriptor pluginDescriptor = getPluginDescriptor();
        PipelinePluginConfigurationState pluginSettings =
                ApplicationManager.getApplication().getService(PipelinePluginConfigurationState.class);
        SentryOptions options = new SentryOptions();
        options.setDsn(pluginSettings.getSentryDsn());
        options.setRelease(pluginDescriptor.getVersion());
        options.setServerName("");
        options.setSendDefaultPii(false);
        options.setEnvironment(pluginDescriptor.getPluginId().getIdString());
        options.setDiagnosticLevel(SentryLevel.ERROR);
        Hub hub = new Hub(options);
        String os = SystemInfo.getOsNameAndVersion() + "-" + SystemInfo.OS_ARCH;
        if (SystemInfo.isLinux) {
            os += (SystemInfo.isChromeOS) ? " [Chrome OS] " : "";
            os += (SystemInfo.isKDE) ? " [KDE] " : "";
            os += (SystemInfo.isGNOME) ? " [GNOME] " : "";
        }
        hub.setTag("os_name_version", os);
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        hub.setTag("jb_platform_type", applicationInfo.getBuild().getProductCode());
        hub.setTag("jb_platform_version", applicationInfo.getBuild().asStringWithoutProductCode());
        hub.setTag("jb_ide", applicationInfo.getVersionName());
	    hub.configureScope(scope -> scope.setUser(null));
        return hub;
    }

}
