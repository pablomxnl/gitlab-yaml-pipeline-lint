package org.ideplugins.plugin.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.ideplugins.plugin.linter.Constants;
import org.jetbrains.annotations.Nullable;


public final class ActionHelper implements Constants {
    private ActionHelper() {
    }

    public static JsonObject getYamlJson(final PsiFile file) {
        String content = file.getText();
        JsonObject result = new JsonObject();
        result.addProperty("content", content);
        return result;
    }

    public static void showLintResult(final JsonObject gitlabResponse, final AnActionEvent actionEvent) {
        if (gitlabResponse.get(GITLAB_RESPONSE_STATUS).getAsInt() == 200) {
            JsonObject result = gitlabResponse.getAsJsonObject(GITLAB_RESPONSE_BODY);
            if (result.has("status")
                    && "valid".equals(result.get("status").getAsString())) {
                getConsole(actionEvent.getProject()).clear();
                displayNotification(NotificationType.INFORMATION,
                        "Congratulations!! your pipeline yaml has no errors");
            } else if (result.has("status")
                    && "invalid".equals(result.get("status").getAsString())) {
                JsonArray errorsArray = result.getAsJsonArray("errors");
                StringBuilder errors = new StringBuilder("gitlab-ci.yml has errors\n" );
                errorsArray.forEach(error -> {
                    errors.append(error.getAsString()).append("\n");
                });
                showResultsInConsole(actionEvent.getProject(), errors.toString(), ConsoleViewContentType.ERROR_OUTPUT);
            }
        } else {
            String result = gitlabResponse.get(GITLAB_RESPONSE_BODY).toString();
            displayNotification(NotificationType.ERROR,
                    "ERROR posting yaml to gitlab lint api; please check plugin settings."
                            + "\n Response status from gitlab: " + gitlabResponse.get(GITLAB_RESPONSE_STATUS).getAsInt()
                            + "\n Response body: \n" + result
            );
        }

    }

    public static void showResultsInConsole(@Nullable Project project, String text, ConsoleViewContentType level){
        ConsoleView consoleView = getConsole(project);
        consoleView.clear();
        consoleView.print(text, level);
        getToolWindow(project).show(null);
    }

    private static ConsoleView getConsole(@Nullable Project project) {
        ToolWindow toolWindow = getToolWindow(project);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.findContent("Lint Results");
        ConsoleView consoleView = (ConsoleView) content.getComponent();
        return consoleView;
    }

    public static ToolWindow getToolWindow(Project project){
        return ToolWindowManager.getInstance(project).getToolWindow("Gitlab Pipeline Lint Results");
    }

    public static void displayNotification(final NotificationType notificationType, final String notificationBody) {
        Notification notification =
                new Notification(GROUP_DISPLAY_ID, NOTIFICATION_TITLE, notificationBody, notificationType);
        Notifications.Bus.notify(notification);
    }
}
