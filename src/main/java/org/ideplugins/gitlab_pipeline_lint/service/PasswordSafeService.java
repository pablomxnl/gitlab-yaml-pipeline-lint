package org.ideplugins.gitlab_pipeline_lint.service;

import com.google.common.base.Throwables;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.ui.Messages;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;

@Service(Service.Level.APP)
public final class PasswordSafeService implements Constants {
    public static void storeToken(final String password) {
        ApplicationManager.getApplication().runWriteAction(
                () -> {
                    try {
                        PasswordSafe.getInstance().setPassword(CREDENTIAL_ATTRIBUTES, password);
                    } catch (Exception e) {
                        Messages.showErrorDialog("Cannot store token\n" + Throwables.getStackTraceAsString(e), "Error");
                    }
                }
        );
    }

    public static String retrieveToken() {
        return PasswordSafe.getInstance().getPassword(CREDENTIAL_ATTRIBUTES);
    }
}
