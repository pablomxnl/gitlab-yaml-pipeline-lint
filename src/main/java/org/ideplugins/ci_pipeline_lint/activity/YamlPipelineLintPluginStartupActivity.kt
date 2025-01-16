package org.ideplugins.ci_pipeline_lint.activity

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore.getPlugin
import com.intellij.notification.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.ide.progress.withBackgroundProgress
import org.ideplugins.ci_pipeline_lint.linter.Constants.*
import org.ideplugins.ci_pipeline_lint.service.PasswordSafeService
import org.ideplugins.ci_pipeline_lint.settings.PipelinePluginConfigurationState
import org.ideplugins.ci_pipeline_lint.settings.YamlPipelineLintSettingsState
import java.util.*

class YamlPipelineLintPluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val pluginId = PluginId.getId(PLUGIN_ID)
        val pluginDescriptor = getPlugin(pluginId)

        if (pluginDescriptor != null) {
            val pluginSettings =
                ApplicationManager.getApplication().getService(
                    PipelinePluginConfigurationState::class.java
                )
            val lastKnownVersion = pluginSettings.lastVersion

            if (lastKnownVersion.isNotEmpty() && lastKnownVersion != pluginDescriptor.version) {
                showUpdateNotification(project, pluginDescriptor, pluginSettings)
            }

            withBackgroundProgress(project, "Migrating settings", false) {
                val oldSettings = PathManager.getOptionsFile("gitlabPipelineYamlLinter")
                val newSettings = PathManager.getOptionsFile("CIPipelineLint")
                if (oldSettings.exists() && !newSettings.exists()){
                    var content = oldSettings.readText(Charsets.UTF_8)
                    val newComponentName = PLUGIN_ID + "-Linter"
                    content = content.replace("\"PluginSettingsState\"", "\"$newComponentName\"")
                    newSettings.writeText(content, Charsets.UTF_8)
                    oldSettings.deleteOnExit()
                }
            }

            withBackgroundProgress(project, "Loading token", false) {
                val settings = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState::class.java)
                settings.gitlabToken = PasswordSafeService.retrieveToken()
            }
        }
    }

}

internal fun showUpdateNotification(
    project: Project, pluginDescriptor: IdeaPluginDescriptor,
    pluginSettings: PipelinePluginConfigurationState
) {
    ApplicationManager.getApplication().invokeLater {
        Optional.ofNullable(
            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
        ).ifPresent { group: NotificationGroup ->
            val action =
                NotificationAction.createSimple(
                    UPDATE_NOTIFICATION_BODY
                ) { BrowserUtil.browse(JB_MARKETPLACE_URL) }
            val notification = group.createNotification(
                UPDATE_NOTIFICATION_TITLE,
                "",
                NotificationType.INFORMATION
            ).addAction(action)
            Notifications.Bus.notify(notification, project)
            pluginSettings.lastVersion = pluginDescriptor.version
        }
    }
}