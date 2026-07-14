package org.ideplugins.ci_pipeline_lint.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.ideplugins.ci_pipeline_lint.linter.Constants;
import org.ideplugins.ci_pipeline_lint.service.PasswordSafeService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ResourceBundle;

public class YamlPipelineLintSettingsConfigurable implements Configurable, Constants {

    private YamlPipelineLintSettingsComponent settingsComponent;
    private final ResourceBundle BUNDLE = ResourceBundle.getBundle(PLUGIN_BUNDLE);

    public YamlPipelineLintSettingsConfigurable(){

    }

    @Override
    public String getDisplayName() {
        return "CI Pipeline Lint";
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
        boolean hostModified = !settingsComponent.getGitlabHost().equals(settingsState.gitlabHost);
        boolean tokenModified = !settingsComponent.getGitlabToken().equals(settingsState.gitlabToken);
        return tokenModified || hostModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        validateValues();
        var settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        settingsState.gitlabHost = settingsComponent.getGitlabHost();
        PasswordSafeService.storeToken(settingsComponent.getGitlabToken());
        settingsState.gitlabToken = settingsComponent.getGitlabToken();
    }

    private void validateValues() throws ConfigurationException {
        StringBuilder errors = new StringBuilder();
        if (settingsComponent.getGitlabHost().isEmpty()) {
            errors.append(BUNDLE.getString("ci.pipeline.lint.plugin.invalid.settings.host.empty"));
        }

        if (settingsComponent.getGitlabToken().isEmpty()) {
            errors.append((errors.isEmpty() ? "" : "\n"))
                    .append(BUNDLE.getString("ci.pipeline.lint.plugin.invalid.settings.token.empty"));
        }

        if (!errors.isEmpty()) {
            throw new ConfigurationException(
                    errors.toString(),
                    BUNDLE.getString("ci.pipeline.lint.plugin.invalid.settings.title"));
        }
    }

    @Override
    public void reset() {
        var settingsState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        settingsComponent.setGitlabToken(settingsState.gitlabToken!=null? settingsState.gitlabToken : "" );
        settingsComponent.setGitlabHost(settingsState.gitlabHost);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
