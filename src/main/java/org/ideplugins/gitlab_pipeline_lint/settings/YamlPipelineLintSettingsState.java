package org.ideplugins.gitlab_pipeline_lint.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
@State(
        name = "PluginSettingsState",
        storages = {@Storage("gitlabPipelineYamlLinter.xml")}
)
public class YamlPipelineLintSettingsState implements PersistentStateComponent<YamlPipelineLintSettingsState> {

    public String gitlabToken = "";
    public String gitlabEndpoint = Constants.GITLAB_URL;


    @Override
    public @Nullable YamlPipelineLintSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull YamlPipelineLintSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
