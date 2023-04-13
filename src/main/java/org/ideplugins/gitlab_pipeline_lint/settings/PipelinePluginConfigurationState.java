package org.ideplugins.gitlab_pipeline_lint.settings;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.ideplugins.gitlab_pipeline_lint.linter.Constants.PLUGIN_ID;

@Service(Service.Level.APP)
@State(name = PLUGIN_ID+"-app", storages = {@Storage("gitlab-pipelineplugin.xml")})
public final class PipelinePluginConfigurationState
        implements PersistentStateComponent<PipelinePluginConfigurationState.PluginSettings> {

    private static final String LAST_VERSION = "lastVersion";
    private static final String SENTRY_DSN = "sentryDsn";

    private PluginSettings settings = createInitialSettings();

    private PluginSettings createInitialSettings() {
        Optional<IdeaPluginDescriptor> optional =
                Optional.ofNullable(PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID)));
        return PluginSettings.create(optional.isPresent()? optional.get().getVersion() : "");
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

    public void setSentryDsn(String dsn){
        settings.configuration.put(SENTRY_DSN, dsn);
    }

    public String getSentryDsn(){
        return settings.configuration.get(SENTRY_DSN);
    }

    static class PluginSettings {
        @MapAnnotation
        private Map<String,String> configuration;

        static PluginSettings create(final String version){
            final PluginSettings instance = new PluginSettings();
            instance.configuration = new TreeMap<>(
                    Map.of(LAST_VERSION, version,
                            SENTRY_DSN , " ")
            );
            return  instance;
        }

    }
}
