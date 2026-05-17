package org.ideplugins.ci_pipeline_lint.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.ideplugins.ci_pipeline_lint.linter.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import static org.ideplugins.ci_pipeline_lint.linter.Constants.*;

@Service(Service.Level.APP)
@State(name = PLUGIN_ID+"-app", storages = {@Storage("gitlab-pipelineplugin.xml")})
public final class PipelinePluginConfigurationState
        implements PersistentStateComponent<PipelinePluginConfigurationState.PluginSettings>, Constants {

    private static final String LAST_VERSION = "lastVersion";
    private static final String SENTRY_DSN = "sentryDsn";

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(PLUGIN_BUNDLE);
    private PluginSettings settings = createInitialSettings();

    private PluginSettings createInitialSettings() {
        return PluginSettings.create(BUNDLE.getString(PLUGIN_VERSION_KEY));
    }

    @Override
    public @Nullable PipelinePluginConfigurationState.PluginSettings getState() {
        return settings;
    }

    @Override
    public void loadState(@NotNull PluginSettings pluginSettings) {
        settings = pluginSettings;
    }

    public String getLastVersion(){
        return settings.configuration.get(LAST_VERSION);
    }

    public void setLastVersion(String version ){
        settings.configuration.put(LAST_VERSION, version);
    }

    public String getSentryDsn(){
        return settings.configuration.get(SENTRY_DSN);
    }

    static class PluginSettings {
        @MapAnnotation
        private Map<String,String> configuration;

        static PluginSettings create(final String version){
            final PluginSettings instance = new PluginSettings();

            String dsn = BUNDLE.getString("sentry.dsn");

            instance.configuration = new TreeMap<>(
                    Map.of(LAST_VERSION, version,
                            SENTRY_DSN , dsn)
            );
            return  instance;
        }

    }
}
