package org.ideplugins.gitlab_pipeline_lint.activity;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.ideplugins.gitlab_pipeline_lint.settings.PipelinePluginConfigurationState;
import org.ideplugins.settings.SettingsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

import static org.ideplugins.gitlab_pipeline_lint.linter.Constants.PLUGIN_ID;

public class YamlPipelineLintPluginStartupActivity implements StartupActivity {

    private static final String UPDATE_NOTIFICATION_TITLE = "Pipeline Lint plugin has been updated!!";
    private static final String JB_MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/19972-gitlab-pipeline-lint/reviews";
    private static final String UPDATE_NOTIFICATION_BODY =
            "Useful? Please rate / review";
    private static final String NOTIFICATION_GROUP = "gitlab-yaml-lint-plugin-update";


    @Override
    public void runActivity(@NotNull Project project) {
        PluginId pluginId = PluginId.getId(PLUGIN_ID);
        IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);

        if (pluginDescriptor != null) {
            PipelinePluginConfigurationState pluginSettings =
                    ApplicationManager.getApplication().getService(PipelinePluginConfigurationState.class);
            String lastKnownVersion = pluginSettings.getLastVersion();
            pluginSettings.setSentryDsn(SettingsProvider.getInstance().getSentryUrl(pluginId.getIdString()));

            if (!lastKnownVersion.isEmpty() && !Objects.equals(lastKnownVersion, pluginDescriptor.getVersion()) ) {
                showUpdateNotification(project, pluginDescriptor, pluginSettings);
            }
        }
    }

    private static void showUpdateNotification(Project project, IdeaPluginDescriptor pluginDescriptor,
                                               PipelinePluginConfigurationState pluginSettings) {
        ApplicationManager.getApplication().invokeLater(() -> Optional.ofNullable(NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)).ifPresent(group -> {

            NotificationAction action =
                    NotificationAction.createSimple(UPDATE_NOTIFICATION_BODY, ()-> BrowserUtil.browse(JB_MARKETPLACE_URL));
            Notification notification = group.createNotification(
                    UPDATE_NOTIFICATION_TITLE,
                    "",
                    NotificationType.INFORMATION).addAction(action);
            Notifications.Bus.notify(notification, project);
            pluginSettings.setLastVersion(pluginDescriptor.getVersion());
        }));
    }
}


