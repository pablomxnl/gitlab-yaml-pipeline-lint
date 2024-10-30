package org.ideplugins.ci_pipeline_lint.service;

import com.google.gson.JsonObject;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class PipelineIssuesReporter {

    private final Project project;

    private final Map<String, List<JsonObject>> issues;

    public PipelineIssuesReporter(Project project) {
        this.project = project;
        issues=new HashMap<>();
    }

    public void populateIssues(Map<String, List<JsonObject>> issueList){
        issues.clear();
        issues.putAll(issueList);
    }

    public boolean hasIssuesForFile(String filePath){
        return issues.containsKey(filePath);
    }

    public List<JsonObject> getIssues(String filePath){
        return issues.get(filePath);
    }

}
