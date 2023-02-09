package org.ideplugins.plugin.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.credentialStore.Credentials;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.ideplugins.plugin.linter.Constants;
import org.ideplugins.plugin.settings.YamlPipelineLintSettingsConfigurable;
import org.ideplugins.plugin.settings.YamlPipelineLintSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public final class ActionHelper implements Constants {
    private ActionHelper() {
    }

    public static JsonObject getYamlJson(final PsiFile file) {
        String content = file.getText();
        return getYamlJson(content);
    }

    public static JsonObject getYamlJson(String body) {
        JsonObject result = new JsonObject();
        result.addProperty("content", body);
        return result;
    }

    public static void showResultsInConsole(@Nullable Project project, String text, ConsoleViewContentType level) {
        ConsoleView consoleView = getConsole(project);
        consoleView.clear();
        consoleView.print(text, level);
        getToolWindow(project).show(null);
    }

    private static ConsoleView getConsole(@Nullable Project project) {
        ToolWindow toolWindow = getToolWindow(project);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.findContent("Lint Results");
        return (ConsoleView) content.getComponent();
    }

    public static ToolWindow getToolWindow(Project project) {
        return ToolWindowManager.getInstance(project).getToolWindow("Gitlab Pipeline Lint Results");
    }

    public static void displayNotification(final NotificationType notificationType, final String notificationBody) {
        Notification notification =
                new Notification(GROUP_DISPLAY_ID, NOTIFICATION_TITLE, notificationBody, notificationType);
        Notifications.Bus.notify(notification);
    }

    public static void displayNotificationWihAction(final NotificationType notificationType, final String notificationBody) {
        Notification notification =
                new Notification(GROUP_DISPLAY_ID, NOTIFICATION_TITLE, notificationBody, notificationType);
        notification.addAction(new NotificationAction("Configure Gitlab Pipeline Lint") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(null,
                        YamlPipelineLintSettingsConfigurable.class);
            }
        });
        Notifications.Bus.notify(notification);
    }

    public static boolean checkGitlabToken() {
        return PasswordSafe.getInstance().get(CREDENTIAL_ATTRIBUTES) != null;
    }

    public static String getGitlabToken() {
        Credentials credentials = PasswordSafe.getInstance().get(CREDENTIAL_ATTRIBUTES);
        return Objects.requireNonNull(credentials).getPasswordAsString();
    }

    public static String getGitlabUrl() {
        return ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class)
                .gitlabEndpoint;

    }

    public static void showLintResult(final JsonObject gitlabResponse, final AnActionEvent actionEvent) {
        JsonObject result = gitlabResponse.getAsJsonObject(GITLAB_RESPONSE_BODY);
        switch (gitlabResponse.get(GITLAB_RESPONSE_STATUS).getAsInt()) {
            case 200:
                if (result.has("status")
                        && "valid".equals(result.get("status").getAsString())) {
                    String successMessage = "✅ Congratulations!! pipeline yaml has no errors";
                    displayNotification(NotificationType.INFORMATION, successMessage);
                    showResultsInConsole(actionEvent.getProject(), successMessage, ConsoleViewContentType.LOG_INFO_OUTPUT);
                } else if (result.has("status")
                        && "invalid".equals(result.get("status").getAsString())) {
                    JsonArray errorsArray = result.getAsJsonArray("errors");
                    StringBuilder errors = new StringBuilder("gitlab-ci.yml has errors\n");
                    errorsArray.forEach(error -> errors.append(error.getAsString()).append("\n"));
                    showResultsInConsole(actionEvent.getProject(), errors.toString(), ConsoleViewContentType.ERROR_OUTPUT);
                }
                break;
            case 401:
                String gitlabHost = gitlabResponse.get("host").getAsString();
                showResultsInConsole(actionEvent.getProject(),
                        String.format("Unauthorized:, click the link in the notification to setup " +
                                "a valid gitlab token for %s\n", gitlabHost) + result,
                        ConsoleViewContentType.ERROR_OUTPUT);
                displayNotificationWihAction(NotificationType.ERROR,
                        String.format("Please configure a valid Gitlab token for %s", gitlabHost));
                break;
            case 408:
                showResultsInConsole(actionEvent.getProject(),
                        String.format("Not able to reach gitlab at %s", gitlabResponse.get("host").getAsString())
                                + "\nPlease check your network or verify the values on plugin settings \n" + result,
                        ConsoleViewContentType.ERROR_OUTPUT);
                displayNotificationWihAction(NotificationType.ERROR, "Double check gitlab host");
                break;
            default:
                displayNotification(NotificationType.ERROR,
                        "ERROR posting yaml to gitlab lint api; please check plugin settings."
                                + "\n Response status from gitlab: " + gitlabResponse.get(GITLAB_RESPONSE_STATUS).getAsInt()
                                + "\n Response body: \n" + result
                );

                break;
        }
    }
}
