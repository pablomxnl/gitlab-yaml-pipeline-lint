package org.ideplugins.ci_pipeline_lint.completion;

import com.google.gson.annotations.SerializedName;

public class GitlabVariable {

    @SerializedName("Variable")
    private String name;

    @SerializedName("Description")
    private String description;

    @SerializedName("Availability")
    private String availability;


    public String getName() {
        return "$" + name;
    }

    public String getDescription() {
        return description;
    }

    public String getAvailability() {
        return availability;
    }

}