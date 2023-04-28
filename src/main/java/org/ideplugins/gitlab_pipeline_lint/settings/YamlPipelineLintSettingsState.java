package org.ideplugins.gitlab_pipeline_lint.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper.getGitlabToken;
import static org.ideplugins.gitlab_pipeline_lint.linter.Constants.GITLAB_HOST;

@Service(Service.Level.PROJECT)
@State(
        name = "PluginSettingsState",
        storages = {@Storage("gitlabPipelineYamlLinter.xml")}
)
public class YamlPipelineLintSettingsState implements PersistentStateComponent<YamlPipelineLintSettingsState> {

    @Transient
    public String gitlabToken = getGitlabToken();
    public String gitlabEndpoint = String.format(Constants.GITLAB_URL, GITLAB_HOST, "%s");
    public String gitlabHost = GITLAB_HOST;

    public String gitlabProjectID = "";


    @Override
    public @Nullable YamlPipelineLintSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull YamlPipelineLintSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
