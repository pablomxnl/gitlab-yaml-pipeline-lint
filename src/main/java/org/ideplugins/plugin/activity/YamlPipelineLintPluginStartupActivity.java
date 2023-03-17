package org.ideplugins.plugin.activity;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.ideplugins.plugin.settings.PipelinePluginConfigurationState;
import org.ideplugins.settings.SettingsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static org.ideplugins.plugin.linter.Constants.PLUGIN_ID;

public class YamlPipelineLintPluginStartupActivity implements StartupActivity {

    private static final String UPDATE_NOTIFICATION_TITLE = "Pipeline Lint plugin has been updated!!";
    private static final String JB_MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/19972-gitlab-pipeline-lint/reviews";
    private static final String UPDATE_NOTIFICATION_BODY =
            "Is it useful? Please <a href='" + JB_MARKETPLACE_URL + "'>Rate / review</a>";
    private static final String NOTIFICATION_GROUP = "gitlab-yaml-lint-plugin-update";


    @Override
    public void runActivity(@NotNull Project project) {
        PluginId pluginId = PluginId.getId(PLUGIN_ID);
        IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);

        if (pluginDescriptor != null) {
            PipelinePluginConfigurationState pluginSettings =
                    ApplicationManager.getApplication().getService(PipelinePluginConfigurationState.class);
            String lastKnownVersion = pluginSettings.getLastVersion();

            initSentrySettings(pluginId.getIdString(), pluginDescriptor, pluginSettings);

            if (!lastKnownVersion.isEmpty() && !lastKnownVersion.equals(pluginDescriptor.getVersion())) {
                showUpdateNotification(project, pluginDescriptor, pluginSettings);
            }
        }
    }

    private static void showUpdateNotification(Project project, IdeaPluginDescriptor pluginDescriptor,
                                               PipelinePluginConfigurationState pluginSettings) {
        ApplicationManager.getApplication().invokeLater(() -> Optional.ofNullable(NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)).ifPresent(group -> {

            Notification notification = group.createNotification(
                            UPDATE_NOTIFICATION_TITLE,
                            UPDATE_NOTIFICATION_BODY,
                            NotificationType.INFORMATION)
                    .setListener(new NotificationListener.UrlOpeningListener(false));
            Notifications.Bus.notify(notification, project);

            pluginSettings.setLastVersion(pluginDescriptor.getVersion());
        }));
    }

    private static void initSentrySettings(String pluginId, IdeaPluginDescriptor pluginDescriptor,
                                           PipelinePluginConfigurationState pluginSettings) {
        String dsn = pluginSettings.getSentryDsn();

        if (dsn.isBlank()){
            dsn = SettingsProvider.getInstance().getSentryUrl(pluginId);
            pluginSettings.setSentryDsn(dsn);
        }
        initSentry(pluginDescriptor, dsn);
    }

    private static void initSentry(IdeaPluginDescriptor pluginDescriptor, String dsn) {
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setRelease(pluginDescriptor.getVersion());
            options.setServerName("");
            options.setEnvironment(pluginDescriptor.getPluginId().getIdString());
            options.setDebug(false);
            options.setDiagnosticLevel(SentryLevel.ERROR);
            options.setShutdownTimeoutMillis(3000);
        });
    }

}


