package org.ideplugins.plugin.annotator;

import com.google.gson.JsonObject;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;

import java.util.List;

class PipelineInitialAnnotatorInfo {

    Document document;
    PsiFile file;
    List<JsonObject> results;


    public PipelineInitialAnnotatorInfo(Document doc, PsiFile psiFile, List<JsonObject> data) {
        document = doc;
        file = psiFile;
        results = data;
    }

}
