package org.ideplugins.plugin.settings;

import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.ideplugins.plugin.linter.Constants;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class YamlPipelineLintSettingsConfigurable implements Configurable, Constants {

    private YamlPipelineLintSettingsComponent settingsComponent;

    @Override
    public String getDisplayName() {
        return "Gitlab-CI Pipeline Lint";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new YamlPipelineLintSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        YamlPipelineLintSettingsState settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        boolean endpointModified = !settingsComponent.getGitlabEndpoint().equals(settingsState.gitlabEndpoint);
        boolean tokenModified = !settingsComponent.getGitlabToken().equals(settingsState.gitlabToken);
        return endpointModified || tokenModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        YamlPipelineLintSettingsState settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        settingsState.gitlabToken = "*".repeat(25);
        Credentials credentials = new Credentials("", settingsComponent.getGitlabToken());
        PasswordSafe.getInstance().set(CREDENTIAL_ATTRIBUTES, credentials);
        settingsState.gitlabEndpoint = settingsComponent.getGitlabEndpoint();
    }

    @Override
    public void reset() {
        YamlPipelineLintSettingsState settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        settingsComponent.setGitlabEndpoint(settingsState.gitlabEndpoint);
        settingsComponent.setGitlabToken(settingsState.gitlabToken);
    }
}
