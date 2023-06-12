package org.ideplugins.gitlab_pipeline_lint.settings;

import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class YamlPipelineLintSettingsConfigurable implements Configurable, Constants {

    private YamlPipelineLintSettingsComponent settingsComponent;

    @Override
    public String getDisplayName() {
        return "Gitlab Pipeline Lint";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new YamlPipelineLintSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }

    @Override
    public boolean isModified() {
        YamlPipelineLintSettingsState settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        boolean endpointModified = !settingsComponent.getGitlabEndpoint().equals(settingsState.gitlabEndpoint);
        boolean hostModified = !settingsComponent.getGitlabHost().equals(settingsState.gitlabHost);
        boolean tokenModified = !settingsComponent.getGitlabToken().equals(settingsState.gitlabToken);
        return endpointModified || tokenModified || hostModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        YamlPipelineLintSettingsState settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        Credentials credentials = new Credentials("", settingsComponent.getGitlabToken());
        PasswordSafe.getInstance().set(CREDENTIAL_ATTRIBUTES, credentials);
        settingsState.setCredentials(credentials);
        settingsState.gitlabHost = settingsComponent.getGitlabHost();
        settingsState.gitlabEndpoint = settingsComponent.getGitlabEndpoint();
        settingsState.gitlabProjectID = settingsComponent.getGitlabProjectID();
    }

    @Override
    public void reset() {
        YamlPipelineLintSettingsState settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        settingsComponent.setGitlabEndpoint(settingsState.gitlabEndpoint);
        settingsComponent.setGitlabToken(ActionHelper.getGitlabToken());
        settingsComponent.setGitlabProjectID(settingsState.gitlabProjectID);
        settingsComponent.setGitlabHost(settingsState.gitlabHost);
    }
}
