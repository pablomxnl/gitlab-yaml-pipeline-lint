package org.ideplugins.gitlab_pipeline_lint.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.ideplugins.gitlab_pipeline_lint.service.PasswordSafeService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class YamlPipelineLintSettingsConfigurable implements Configurable, Constants {

    private YamlPipelineLintSettingsComponent settingsComponent;

    public YamlPipelineLintSettingsConfigurable(){

    }

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
        var settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        boolean endpointModified = !settingsComponent.getGitlabEndpoint().equals(settingsState.gitlabEndpoint);
        boolean hostModified = !settingsComponent.getGitlabHost().equals(settingsState.gitlabHost);
        boolean tokenModified = !settingsComponent.getGitlabToken().equals(settingsState.gitlabToken);
        return endpointModified || tokenModified || hostModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        var settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        settingsState.gitlabHost = settingsComponent.getGitlabHost();
        settingsState.gitlabEndpoint = settingsComponent.getGitlabEndpoint();
        settingsState.gitlabProjectID = settingsComponent.getGitlabProjectID();
        PasswordSafeService.storeToken(settingsComponent.getGitlabToken());
        settingsState.gitlabToken = settingsComponent.getGitlabToken();
    }

    @Override
    public void reset() {
        var settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        settingsComponent.setGitlabEndpoint(settingsState.gitlabEndpoint);
        settingsComponent.setGitlabToken(settingsState.gitlabToken);
        settingsComponent.setGitlabProjectID(settingsState.gitlabProjectID);
        settingsComponent.setGitlabHost(settingsState.gitlabHost);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
