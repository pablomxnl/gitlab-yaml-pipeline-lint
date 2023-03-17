package org.ideplugins.plugin.annotator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.ideplugins.plugin.service.PipelineIssuesReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YamlRecursivePsiElementVisitor;

import java.util.List;
import java.util.Optional;

import static com.intellij.lang.annotation.HighlightSeverity.ERROR;
import static com.intellij.lang.annotation.HighlightSeverity.WARNING;
import static org.ideplugins.plugin.linter.Constants.GITLAB_RESPONSE_BODY;

public class PipelineLintResultsExternalAnnotator extends ExternalAnnotator<PipelineInitialAnnotatorInfo, PipelineLintResult> {

    @Override
    public @Nullable PipelineInitialAnnotatorInfo collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        PipelineIssuesReporter pipelineServiceReporter = file.getProject().getService(PipelineIssuesReporter.class);
        Document document = editor.getDocument();
        if (!pipelineServiceReporter.hasIssuesForFile(file.getVirtualFile().getPath()))
            return null;

        return new PipelineInitialAnnotatorInfo(document, file,
                pipelineServiceReporter.getIssues(file.getVirtualFile().getPath()));
    }

    @Override
    public @Nullable PipelineLintResult doAnnotate(PipelineInitialAnnotatorInfo collectedInfo) {
        return new PipelineLintResult(collectedInfo);
    }

    @Override
    public void apply(@NotNull PsiFile file, PipelineLintResult annotationResult,
                      @NotNull AnnotationHolder holder) {
        List<JsonObject> lintresults = annotationResult.getLintResults();
        JsonObject result = lintresults.get(0).getAsJsonObject(GITLAB_RESPONSE_BODY);
        JsonArray errorsArray = result.getAsJsonArray("errors");
        JsonArray warningsArray = result.getAsJsonArray("warnings");
        iterateIssues(file, errorsArray, ERROR, holder);
        iterateIssues(file, warningsArray, WARNING, holder);
//        dummy error for testing
//        String test = null;
//        System.out.println(test.indexOf("1"));
    }

    private void iterateIssues(PsiFile file, JsonArray messagesArray,
                               HighlightSeverity severity, AnnotationHolder holder) {
        Optional.ofNullable(messagesArray).ifPresent(array -> {
            array.forEach(message -> {
                String errorString = message.getAsString();
                String messageLine = errorString;
                if (errorString.startsWith("jobs:")) {
                    messageLine = errorString.substring(5);
                }
                String word = messageLine.split(" ", 2)[0];
                String job;
                if (word.indexOf(':') == -1) {
                    job = word;
                } else {
                    job = word.substring(0, word.indexOf(':'));
                }
                String errorMessage = messageLine.substring(job.length() + 1);
                file.accept(new PipelineYamlRecursivePsiElementVisitor(job, holder, severity, errorMessage));
            });
        });

    }

    private static class PipelineYamlRecursivePsiElementVisitor extends YamlRecursivePsiElementVisitor {
        private final String job;
        private final @NotNull AnnotationHolder holder;
        private final HighlightSeverity severity;
        private final String errorMessage;

        public PipelineYamlRecursivePsiElementVisitor(String job, @NotNull AnnotationHolder holder,
                                                      HighlightSeverity severity, String errorMessage) {
            this.job = job;
            this.holder = holder;
            this.severity = severity;
            this.errorMessage = errorMessage;
        }

        @Override
        public void visitKeyValue(@NotNull YAMLKeyValue keyValue) {
            Optional.ofNullable(keyValue.getKey()).ifPresent(key -> {
                String text = key.getText();
                if (job.equals(text)) {
                    holder.newAnnotation(severity, job + " " + errorMessage)
                            .tooltip(job + " " + errorMessage)
                            .range(keyValue).create();
                }
            });

        }
    }
}
