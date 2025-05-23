package org.ideplugins.ci_pipeline_lint.annotator;

import com.google.gson.JsonObject;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.ideplugins.ci_pipeline_lint.actions.ActionHelper;
import org.ideplugins.ci_pipeline_lint.linter.YamlPipelineLinter;
import org.ideplugins.ci_pipeline_lint.service.PipelineIssuesReporter;
import org.ideplugins.ci_pipeline_lint.testing.PluginTest;
import org.ideplugins.ci_pipeline_lint.testing.RunInEdtExtension;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@PluginTest
@ExtendWith(RunInEdtExtension.class)
public class PipelineLintResultsExternalAnnotatorTest {

    private MockWebServer mockWebServer;
    private YamlPipelineLinter pipelineLinter;

    @BeforeEach
    public void setup(CodeInsightTestFixture myFixture) throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        pipelineLinter = new YamlPipelineLinter(mockWebServer.url("/").toString(), "");
        myFixture.copyDirectoryToProject("annotator", "src");
    }

    @Test
    public void testAnnotator(CodeInsightTestFixture myFixture) {
        PipelineLintResultsExternalAnnotator annotator = new PipelineLintResultsExternalAnnotator();
        PsiFile file = myFixture.configureByFile("src/.gitlab-ci.yml");
        PsiFile expectedResponse = myFixture.configureByFile("src/gitlab_ci_lint_response.json");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(expectedResponse.getText()));
        JsonObject linterResult = pipelineLinter.ciLint(ActionHelper.getYamlJson(file));
        PipelineIssuesReporter reporter = myFixture.getProject().getService(PipelineIssuesReporter.class);
        reporter.populateIssues(
                Map.of(file.getVirtualFile().getPath(), List.of(linterResult))
        );
        PipelineInitialAnnotatorInfo info = annotator.collectInformation(file, myFixture.getEditor(), false);
        PipelineLintResult result = annotator.doAnnotate(info);
        Assertions.assertNotNull(result, "Annotation result shouldn't be null");
        AnnotationHolder holder = mock(AnnotationHolder.class);
        AnnotationBuilder builder = mock(AnnotationBuilder.class);
        when(holder.newAnnotation(any(HighlightSeverity.class), anyString())).thenReturn(builder);
        when(builder.tooltip(anyString())).thenReturn(builder);
        when(builder.range(any(YAMLKeyValue.class))).thenReturn(builder);
        ApplicationManager.getApplication().runReadAction(() -> annotator.apply(file, result, holder));
        String error1 = "scheduled_regression_test config contains unknown keys: bedfore_script";
        String error2 = "scheduled_regression_qa config contains unknown keys: bedfore_script";
        String warning1 = "my_firs_tjob may allow multiple pipelines to run for a single action due to `rules:when` clause with no `workflow:rules` - read more: https://docs.gitlab.com/ee/ci/troubleshooting.html#pipeline-warnings";
        String warning2 = "my_second_job may allow multiple pipelines to run for a single action due to `rules:when` clause with no `workflow:rules` - read more: https://docs.gitlab.com/ee/ci/troubleshooting.html#pipeline-warnings";
        verify(holder, times(1)).newAnnotation(HighlightSeverity.ERROR, error1);
        verify(holder, times(1)).newAnnotation(HighlightSeverity.ERROR, error2);
        verify(holder, times(1)).newAnnotation(HighlightSeverity.WARNING, warning1);
        verify(holder, times(1)).newAnnotation(HighlightSeverity.WARNING, warning2);
        verify(builder, times(4)).tooltip(anyString());

    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }
}