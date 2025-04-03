package org.ideplugins.ci_pipeline_lint.errorhandling;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.InstalledPluginsState;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.Consumer;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.ideplugins.ci_pipeline_lint.settings.PipelinePluginConfigurationState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

import static org.ideplugins.ci_pipeline_lint.actions.ActionHelper.displayNotificationWithAction;


public class SentryErrorReporter extends ErrorReportSubmitter {

    private static boolean isSentryInit;


    private static void submitErrors(IdeaLoggingEvent @NotNull [] events, String additionalInfo) {
        for (IdeaLoggingEvent ideaEvent : events) {
            Sentry.captureMessage(ideaEvent.getThrowableText(),
                    SentryLevel.ERROR, cb -> {
                        cb.setExtra("userMessage", additionalInfo);
                        cb.setUser(null);
                    });
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
        PluginDescriptor pluginDescriptor = getPluginDescriptor();
        Project project = CommonDataKeys.PROJECT.getData(context);
        initSentry(pluginDescriptor);
        InstalledPluginsState pluginState = InstalledPluginsState.getInstance();
        if (pluginState.hasNewerVersion(pluginDescriptor.getPluginId())) {
            showOutdatedPluginErrorNotification(pluginDescriptor);
            consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.DUPLICATE));
            return true;
        }
        new Task.Backgroundable(project, "Sending error report") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    submitErrors(events, additionalInfo);
                    Messages.showInfoMessage(parentComponent,
                            "Thanks!! Error will be reviewed in a few days.", "Error Report Submitted");
                    consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
                });
            }
        }.queue();

        return true;
    }

    private void showOutdatedPluginErrorNotification(PluginDescriptor descriptor) {
        ApplicationManager.getApplication().invokeLater(() ->
                displayNotificationWithAction(NotificationType.ERROR,
                        "Error won't be submitted because there is a newer version available",
                        "Update %s Plugin".formatted(descriptor.getName()),
                        () ->
                                ShowSettingsUtil.getInstance()
                                        .showSettingsDialog(null, IdeBundle.message("title.plugins"))
                )
        );
    }

    static synchronized void initSentry(final PluginDescriptor pluginDescriptor) {
        if (!isSentryInit) {
            PipelinePluginConfigurationState pluginSettings =
                    ApplicationManager.getApplication().getService(PipelinePluginConfigurationState.class);

            ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
            String os = SystemInfo.getOsNameAndVersion() + "-" + SystemInfo.OS_ARCH;
            if (SystemInfo.isLinux) {
                os += (SystemInfo.isChromeOS) ? " [Chrome OS] " : "";
                os += (SystemInfo.isKDE) ? " [KDE] " : "";
                os += (SystemInfo.isGNOME) ? " [GNOME] " : "";
            }
            final String operatingSystem = os;
            Sentry.init(options -> {
                options.setDsn(pluginSettings.getSentryDsn());
                options.setRelease(pluginDescriptor.getVersion());
                options.setServerName("");
                options.setSendDefaultPii(false);
                options.setEnvironment(pluginDescriptor.getPluginId().getIdString());
                options.setDiagnosticLevel(SentryLevel.ERROR);
            });

            Sentry.configureScope(scope -> {
                scope.setUser(null);
                scope.setTag("os_name_version", operatingSystem);
                scope.setTag("jb_platform_type", applicationInfo.getBuild().getProductCode());
                scope.setTag("jb_platform_version", applicationInfo.getBuild().asStringWithoutProductCode());
                scope.setTag("jb_ide", applicationInfo.getVersionName());

            });
            isSentryInit = true;
        }
    }

}
