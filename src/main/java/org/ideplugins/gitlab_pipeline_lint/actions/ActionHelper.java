package org.ideplugins.gitlab_pipeline_lint.actions;

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
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.ideplugins.gitlab_pipeline_lint.settings.YamlPipelineLintSettingsConfigurable;
import org.ideplugins.gitlab_pipeline_lint.settings.YamlPipelineLintSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.intellij.execution.ui.ConsoleViewContentType.ERROR_OUTPUT;
import static com.intellij.execution.ui.ConsoleViewContentType.LOG_INFO_OUTPUT;


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

    public static void displayNotificationWithAction(final NotificationType notificationType, final String notificationBody) {
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
        String token = "";
        Optional<Credentials> credentials = Optional.ofNullable(PasswordSafe.getInstance().get(CREDENTIAL_ATTRIBUTES));
        if (credentials.isPresent()){
            token = credentials.get().getPasswordAsString();
        }
        return token;
    }

    public static String getGitlabUrl() {
        return ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class)
                .gitlabEndpoint;

    }

    public static void showLintResult(final JsonObject gitlabResponse, final AnActionEvent actionEvent) {
        JsonObject result = gitlabResponse.getAsJsonObject(GITLAB_RESPONSE_BODY);
        String gitlabHost = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class)
                .gitlabHost;
        switch (gitlabResponse.get(GITLAB_RESPONSE_STATUS).getAsInt()) {
            case HttpURLConnection.HTTP_OK :
                if (    result.has("valid")
                        && result.get("valid").getAsBoolean() &&
                        result.get("warnings").getAsJsonArray().isEmpty()
                ) {

                    String successMessage = "✅ Congratulations!! Pipeline yaml has no errors";
                    showResultsInConsole(actionEvent.getProject(), successMessage, LOG_INFO_OUTPUT);
                } else if (result.has("valid")
                        && result.get("valid").getAsBoolean()  &&
                        !result.get("warnings").getAsJsonArray().isEmpty()){

                    showResultsInConsole(actionEvent, result, "warnings");

                } else if (result.has("valid")
                        && !result.get("valid").getAsBoolean() ) {
                    showResultsInConsole(actionEvent, result, "warnings", "errors");
                }
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                showResultsInConsole(actionEvent.getProject(),
                        String.format("Unauthorized:, click the link in the notification to setup " +
                                "a valid gitlab token for %s\n", gitlabHost) + result,
                        ERROR_OUTPUT);
                displayNotificationWithAction(NotificationType.ERROR,
                        String.format("Please configure a valid Gitlab token for %s", gitlabHost));
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
                showResultsInConsole(actionEvent.getProject(),
                        String.format("""
                                Project ID not found:
                                Please double check your project ID at %s, click the link in the notification to set it up\s
                                """, gitlabHost) + result,
                        ERROR_OUTPUT);
                displayNotificationWithAction(NotificationType.ERROR,
                        String.format("Please configure Gitlab Project ID at %s", gitlabHost));
                break;
            case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
                showResultsInConsole(actionEvent.getProject(),
                        String.format("Not able to reach gitlab at %s", gitlabHost)
                                + "\nPlease check your network or verify the values on plugin settings \n" + result,
                        ERROR_OUTPUT);
                displayNotificationWithAction(NotificationType.ERROR, "Double check gitlab host");
                break;
            default:
                showResultsInConsole(actionEvent.getProject(),
                        String.format("ERROR using gitlab lint api at %s", gitlabHost)
                                + "\nplease check plugin settings \n" + result,
                        ERROR_OUTPUT);
                displayNotificationWithAction(NotificationType.ERROR,
                        "ERROR using gitlab lint api; please check plugin settings."
                                + "\n Response body: \n" + result
                );

                break;
        }
    }

    private static void showResultsInConsole(AnActionEvent actionEvent, JsonObject result, String ... errorTypes) {
        Set<String> processedErrorType = new HashSet<>();
        StringBuilder errors = new StringBuilder();
        for (String errorType : errorTypes) {
            JsonArray errorsArray = result.getAsJsonArray(errorType);
            if (!errorsArray.isEmpty()){
                errors.append(errorType).append(":\n");
            }
            errorsArray.forEach(error -> {
                errors.append(error.getAsString()).append("\n");
                processedErrorType.add(errorType);
            });
            errors.append("\n");
        }
        String successMessage = "";
        ConsoleViewContentType level = LOG_INFO_OUTPUT;
        if (errorTypes.length ==1) {
            successMessage = "⚠️ Pipeline yaml has warnings";
        }
        if (errorTypes.length == 2){
            successMessage = processedErrorType.size()==2 ?
                    "❌⚠️ Pipeline yaml has errors and warnings":
                    "❌ Pipeline yaml has errors";
            level = ERROR_OUTPUT;
        }

        showResultsInConsole(actionEvent.getProject(), successMessage + "\n" + errors, level);
    }
}
