package org.ideplugins.ci_pipeline_lint.activity

import com.intellij.ide.BrowserUtil
import com.intellij.notification.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.PathManager.DEFAULT_EXT
import com.intellij.openapi.application.PathManager.OPTIONS_DIRECTORY
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.ide.progress.withBackgroundProgress
import org.ideplugins.ci_pipeline_lint.linter.Constants
import org.ideplugins.ci_pipeline_lint.linter.Constants.*
import org.ideplugins.ci_pipeline_lint.service.PasswordSafeService
import org.ideplugins.ci_pipeline_lint.settings.PipelinePluginConfigurationState
import org.ideplugins.ci_pipeline_lint.settings.YamlPipelineLintSettingsState
import org.ideplugins.ci_pipeline_lint.settings.ProjectGitSettingsState
import org.ideplugins.ci_pipeline_lint.settings.GitRemoteReader
import org.ideplugins.ci_pipeline_lint.settings.GitlabRemoteParser
import com.intellij.openapi.diagnostic.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.*

class YamlPipelineLintPluginStartupActivity : ProjectActivity, Constants {

    companion object {
        private val LOG: Logger = Logger.getInstance(YamlPipelineLintPluginStartupActivity::class.java)
    }

    private val bundle: ResourceBundle get() = ResourceBundle.getBundle(PLUGIN_BUNDLE)

    override suspend fun execute(project: Project) {
        val pluginSettings =
            ApplicationManager.getApplication().getService(
                PipelinePluginConfigurationState::class.java
            )
        val lastKnownVersion = pluginSettings.lastVersion

        if (lastKnownVersion.isNotEmpty() && lastKnownVersion != bundle.getString(PLUGIN_VERSION_KEY)) {
            pluginSettings.lastVersion = bundle.getString(PLUGIN_VERSION_KEY)
            showUpdateNotification(project)
        }

        withBackgroundProgress(project, "Migrating settings", false) {
            val optionsPath = PathManager.getConfigDir().resolve(OPTIONS_DIRECTORY)
            val oldSettings = optionsPath.resolve("gitlabPipelineYamlLinter$DEFAULT_EXT").toFile()
            val newSettings = optionsPath.resolve("CIPipelineLint$DEFAULT_EXT").toFile()
            if (oldSettings.exists() && !newSettings.exists()){
                var content = oldSettings.readText(Charsets.UTF_8)
                val newComponentName = "$PLUGIN_ID-Linter"
                content = content.replace("\"PluginSettingsState\"", "\"$newComponentName\"")
                newSettings.writeText(content, Charsets.UTF_8)
                oldSettings.deleteOnExit()
            }
        }

        withBackgroundProgress(project, "Loading token", false) {
            val settings = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState::class.java)
            settings.gitlabToken = PasswordSafeService.retrieveToken()
        }

        withBackgroundProgress(project, "Detecting git remote and project id", false) {
            try {
                val projectState = project.getService(ProjectGitSettingsState::class.java)
                // If project ID is already persisted, skip detection
                if (!projectState.projectId.isNullOrBlank()) {
                    return@withBackgroundProgress
                }

                val settings = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState::class.java)
                val token = settings.gitlabToken

                val remoteUrl = GitRemoteReader.getRemoteUrl(project)
                if (remoteUrl == null) {
                    // Could not detect remote — don't prompt the user at startup, only log a warning
                    LOG.warn("Could not detect git remote for project '${project.name}'. Skipping GitLab project auto-detection.")
                    return@withBackgroundProgress
                }

                val remoteInfo = GitlabRemoteParser.parse(remoteUrl)
                if (remoteInfo == null) {
                    // Remote is not a GitLab URL or couldn't be parsed — log and skip prompting at startup
                    LOG.warn("Git remote '$remoteUrl' for project '${project.name}' is not a recognized GitLab remote. Skipping GitLab project auto-detection.")
                    return@withBackgroundProgress
                }

                // If we have a token, try to resolve project id via GitLab API
                if (token != null && token.isNotEmpty()) {
                    try {
                        val projectId = fetchProjectId(remoteInfo.host, remoteInfo.projectPath, token)
                        if (projectId != null) {
                            projectState.remoteUrl = remoteUrl
                            projectState.projectId = projectId.toString()
                            return@withBackgroundProgress
                        } else {
                            LOG.warn("Could not resolve GitLab project id for '${remoteInfo.projectPath}' on host '${remoteInfo.host}' for project '${project.name}'.")
                        }
                    } catch (e: Exception) {
                        LOG.warn("Error while resolving GitLab project id for project '${project.name}': ${e.message}", e)
                    }
                } else {
                    LOG.warn("No GitLab token configured for project '${project.name}'. Skipping GitLab project auto-detection.")
                }
            } catch (t: Throwable) {
                // best effort — do not block startup on detection errors
            }
        }

    }

}

internal fun showUpdateNotification(
    project: Project
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
        }
    }
}


@Throws(Exception::class)
private fun fetchProjectId(host: String, projectPath: String, token: String): Int? {
    val encoded = java.net.URLEncoder.encode(projectPath, Charsets.UTF_8).replace("+", "%20")
    val url = "https://$host/api/v4/projects/$encoded"
    val client = OkHttpClient.Builder().build()
    val request = Request.Builder()
        .url(url)
        .addHeader("PRIVATE-TOKEN", token)
        .build()

    client.newCall(request).execute().use { response: Response ->
        if (!response.isSuccessful) return null
        val body = response.body.string()
        val json = com.google.gson.JsonParser.parseString(body).asJsonObject
        return if (json.has("id")) json.get("id").asInt else null
    }
}
