package org.ideplugins.ci_pipeline_lint.util;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.ideplugins.ci_pipeline_lint.linter.Constants;
import org.ideplugins.ci_pipeline_lint.settings.ProjectGitSettingsState;

import java.util.Optional;

public class NotificationHelpers {

    public enum NotifyProjectIdReason {
        MISSING_HOST_OR_TOKEN,
        MISSING_PROJECT_ID
    }

    public static void notifyAskForProjectId(Project project, NotifyProjectIdReason reason) {
        ApplicationManager.getApplication().invokeLater(() -> Optional.ofNullable(NotificationGroupManager.getInstance().getNotificationGroup(Constants.NOTIFICATION_GROUP))
                .ifPresent(group -> {
                    String title;
                    String content;

                    if (reason == NotifyProjectIdReason.MISSING_HOST_OR_TOKEN) {
                        title = "GitLab configuration";
                        content = "GitLab host or token is missing. Configure the host/token in plugin settings or enter the Project ID manually.";
                    } else {
                        title = "GitLab project detection";
                        content = "Could not detect GitLab project ID automatically. You can enter the Project ID manually.";
                    }

                    NotificationAction action = NotificationAction.createSimple("Enter Project ID", () ->
                            ApplicationManager.getApplication().invokeLater(() -> {
                                String input = Messages.showInputDialog(project,
                                        content + "\nPlease enter Project ID:",
                                        "Enter Project ID",
                                        null);
                                if (input != null && !input.isBlank()) {
                                    ProjectGitSettingsState projectState = project.getService(ProjectGitSettingsState.class);
                                    projectState.projectId = input.trim();
                                }
                            })
                    );

                    Notification notification = group.createNotification(
                            title,
                            content,
                            NotificationType.WARNING
                    ).addAction(action);

                    Notifications.Bus.notify(notification, project);
                }));
    }
}
