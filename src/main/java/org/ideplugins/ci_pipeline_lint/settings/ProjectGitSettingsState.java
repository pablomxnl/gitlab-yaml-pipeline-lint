package org.ideplugins.ci_pipeline_lint.settings;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
@State(
        name = "org.ideplugins.ci_pipeline_lint_project_settings",
        storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)}
)
public final class ProjectGitSettingsState implements PersistentStateComponent<ProjectGitSettingsState> {

    public String remoteUrl = "";
    public String projectId = "";

    @Override
    public ProjectGitSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ProjectGitSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}

