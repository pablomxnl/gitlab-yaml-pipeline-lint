package org.ideplugins.gitlab_pipeline_lint.settings;

import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.components.SettingsCategory.TOOLS;
import static org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper.getGitlabToken;
import static org.ideplugins.gitlab_pipeline_lint.linter.Constants.CREDENTIAL_ATTRIBUTES;
import static org.ideplugins.gitlab_pipeline_lint.linter.Constants.GITLAB_HOST;

@Service(Service.Level.PROJECT)
@State(
        name = "PluginSettingsState",
        storages = {@Storage("gitlabPipelineYamlLinter.xml")},
        category = TOOLS ,
        reloadable = true
)
public class YamlPipelineLintSettingsState implements PersistentStateComponent<YamlPipelineLintSettingsState> {

    @Transient
    public String gitlabToken = getGitlabToken();
    public String gitlabEndpoint = String.format(Constants.GITLAB_URL, GITLAB_HOST, "%s");
    public String gitlabHost = GITLAB_HOST;

    public String gitlabProjectID = "";

    private Credentials credentials = PasswordSafe.getInstance().get(CREDENTIAL_ATTRIBUTES);

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public @Nullable YamlPipelineLintSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull YamlPipelineLintSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}
