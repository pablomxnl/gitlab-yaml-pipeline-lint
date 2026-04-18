package org.ideplugins.ci_pipeline_lint.annotator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.junit5.StartStop;
import org.ideplugins.ci_pipeline_lint.actions.ActionHelper;
import org.ideplugins.ci_pipeline_lint.linter.YamlPipelineLinter;
import org.ideplugins.ci_pipeline_lint.service.PipelineIssuesReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.intellij.testFramework.common.ThreadLeakTracker.longRunningThreadCreated;

public class PipelineLintResultsExternalAnnotatorTest extends BasePlatformTestCase {

    public static final String PIPELINE_FILE_CONTENT = """
            stages:
              - build
            
            my_firs_tjob:
              stage: build
              script:
                - ls
              rules:
                #    - if: $CI_PIPELINE_SOURCE == "push"
                #    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
                - when: always
            
            my_second_job:
              stage: build
              script:
                - ls
              rules:
                #    - if: $CI_PIPELINE_SOURCE == "push"
                #    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
                - when: always
            
            compile:
              only:
                refs:
                  - master
                  - merge_requests
                  - api
                  - web
              except:
                refs:
                  - schedules
                variables:
                  - $CI_MERGE_REQUEST_TITLE =~ /^WIP:.*/
              tags:
                - docker
              stage: build
              variables:
                TEST_ENVIRONMENT: 'testing'
              before_script:
                - echo "before_tests"
              script:
                - cd myproject
                - mvn -B -q test -Denv="${TEST_ENVIRONMENT}"
              artifacts:
                paths:
                  - myproject/target/surefire-reports
                reports:
                  junit: myproject/target/surefire-reports/junitreports/*.xml
                when: always
                expire_in: 7 days
            
            .scheduled_pipeline:
              only:
                refs:
                  - schedules
              bedfore_script:
                - echo "before_tests"
              variables:
                TEST_ENVIRONMENT: 'dev'
              script:
                - cd myproject
                - mvn -B -q test -Denv=${TEST_ENVIRONMENT} -DtestSuite=MySuite
              artifacts:
                when: always
                reports:
                  junit: myproject/target/surefire-reports/junitreports/*.xml
                paths:
                  - myproject/target/*.csv
                  - myproject/target/surefire-reports
                expire_in: 7 days
              stage: build
            
            scheduled_regression_test:
              extends: .scheduled_pipeline
              only:
                variables:
                  - $TEST_JOB == "REGRESSION_DEV"
              tags:
                - docker
            
            scheduled_regression_qa:
              extends: .scheduled_pipeline
              variables:
                TEST_ENVIRONMENT: 'qa'
              only:
                variables:
                  - $TEST_JOB == "REGRESSION_QA"
              tags:
                - docker
            """;

    @StartStop
    private final MockWebServer mockWebServer = new MockWebServer();

    private YamlPipelineLinter pipelineLinter;
    private PsiFile file;
    private PsiFile expectedResponse;

    private static final String GITLAB_API_RESPONSE = """
            {
              "valid": false,
              "errors": [
                "jobs:scheduled_regression_test config contains unknown keys: bedfore_script",
                "jobs:scheduled_regression_qa config contains unknown keys: bedfore_script"
              ],
              "warnings": [
                "jobs:my_firs_tjob may allow multiple pipelines to run for a single action due to `rules:when` clause with no `workflow:rules` - read more: https://docs.gitlab.com/ee/ci/troubleshooting.html#pipeline-warnings",
                "jobs:my_second_job may allow multiple pipelines to run for a single action due to `rules:when` clause with no `workflow:rules` - read more: https://docs.gitlab.com/ee/ci/troubleshooting.html#pipeline-warnings"
              ],
              "includes": [],
              "status": "invalid"
            }""";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Application app = ApplicationManager.getApplication();
        for (String s : Arrays.asList("SystemPropertyWatcher", "OkHttp", "Okio", "MockWebServer")) {
            //noinspection UnstableApiUsage
            longRunningThreadCreated(app, s);
        }
        mockWebServer.start();
        pipelineLinter = new YamlPipelineLinter(mockWebServer.url("/").toString(), "");
        file = myFixture.configureByText(".gitlab-ci.yml", PIPELINE_FILE_CONTENT);
        expectedResponse = myFixture.configureByText("gitlab_ci_lint_response.json", GITLAB_API_RESPONSE);
        mockWebServer.enqueue(new MockResponse.Builder().code(200).body(expectedResponse.getText()).build());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mockWebServer.close();
    }

    public void testAnnotator() {
        PipelineLintResultsExternalAnnotator annotator = new PipelineLintResultsExternalAnnotator();
        JsonObject linterResult = pipelineLinter.ciLint(ActionHelper.getYamlJson(file));
        PipelineIssuesReporter reporter = myFixture.getProject().getService(PipelineIssuesReporter.class);
        reporter.populateIssues(
                Map.of(file.getVirtualFile().getPath(), List.of(linterResult))
        );

        PipelineInitialAnnotatorInfo info = annotator.collectInformation(file, myFixture.getEditor(), false);

        AtomicReference<PipelineLintResult> result = new AtomicReference<>();
        List<Annotation> annotations = CodeInsightTestUtil.runExternalAnnotator(annotator, file, info, result::set);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        assertEquals(result.get().getLintResults().getFirst(), linterResult);
        assertEquals(4, annotations.size());
        assertEquals("YAML", file.getFileType().getName());
        assertEquals(expectedResponse.getText(),
                gson.toJson(result.get().getLintResults().getFirst().get("gitlabResponse")));
    }
}
