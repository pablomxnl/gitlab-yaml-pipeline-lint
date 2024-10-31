package org.ideplugins.ci_pipeline_lint.completion;

import com.google.gson.annotations.SerializedName;

public class GitlabVariable {

    @SerializedName("Variable")
    private String name;

    @SerializedName("Description")
    private String description;

    @SerializedName("GitLab")
    private String gitlabVersion;

    @SerializedName("Runner")
    private String runnerVersion;

    public String getName() {
        return "$" + name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return gitlabVersion;
    }

    public String getRunner() {
        return runnerVersion;
    }
}