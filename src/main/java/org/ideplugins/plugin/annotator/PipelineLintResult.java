package org.ideplugins.plugin.annotator;

import com.google.gson.JsonObject;

import java.util.List;

public class PipelineLintResult {

    private final PipelineInitialAnnotatorInfo info;


    public PipelineLintResult(PipelineInitialAnnotatorInfo info) {
        this.info = info;
    }

    public List<JsonObject> getLintResults(){
        return info.results;
    }
}
