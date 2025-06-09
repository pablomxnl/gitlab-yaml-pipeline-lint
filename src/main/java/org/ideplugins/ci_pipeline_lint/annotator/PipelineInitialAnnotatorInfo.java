package org.ideplugins.ci_pipeline_lint.annotator;

import com.google.gson.JsonObject;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;

import java.util.List;

public class PipelineInitialAnnotatorInfo {

    final Document document;
    final PsiFile file;
    final List<JsonObject> results;


    public PipelineInitialAnnotatorInfo(Document doc, PsiFile psiFile, List<JsonObject> data) {
        document = doc;
        file = psiFile;
        results = data;
    }

}
