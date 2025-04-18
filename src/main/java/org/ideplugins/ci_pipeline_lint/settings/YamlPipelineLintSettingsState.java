package org.ideplugins.ci_pipeline_lint.settings;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.ideplugins.ci_pipeline_lint.linter.Constants;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.components.SettingsCategory.TOOLS;
import static org.ideplugins.ci_pipeline_lint.linter.Constants.GITLAB_HOST;
import static org.ideplugins.ci_pipeline_lint.linter.Constants.PLUGIN_ID;

@Service
@State(
        name = PLUGIN_ID+"-Linter",
        storages = {@Storage("CIPipelineLint.xml")},
        category = TOOLS
)
public final class YamlPipelineLintSettingsState implements PersistentStateComponent<YamlPipelineLintSettingsState> {

    @Transient
    public String gitlabToken = "";
    public String gitlabEndpoint = String.format(Constants.GITLAB_URL, GITLAB_HOST, "%s");
    public String gitlabHost = GITLAB_HOST;
    public String gitlabProjectID = "";


    @Override
    public YamlPipelineLintSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull YamlPipelineLintSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }



}
