package org.ideplugins.ci_pipeline_lint.linter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.junit5.StartStop;
import org.apache.http.HttpStatus;
import org.ideplugins.ci_pipeline_lint.actions.ActionHelper;
import org.ideplugins.ci_pipeline_lint.testing.PluginTest;
import org.ideplugins.ci_pipeline_lint.testing.RunInEdtExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.intellij.testFramework.common.ThreadLeakTracker.longRunningThreadCreated;
import static org.junit.jupiter.api.Assertions.*;

@PluginTest
@ExtendWith(RunInEdtExtension.class)
class YamlPipelineLinterTest implements Constants {

    @StartStop
    public final MockWebServer mockWebServer = new MockWebServer();


    private YamlPipelineLinter pipelineLinter;

    @BeforeEach
    public void setup() throws IOException {
        Application app = ApplicationManager.getApplication();
        for (String s : Arrays.asList("SystemPropertyWatcher", "OkHttp", "MockWebServer", "Okio")) {
            //noinspection UnstableApiUsage
            longRunningThreadCreated(app, s);
        }
        pipelineLinter = new YamlPipelineLinter(mockWebServer.url("/").toString(), "");
    }

    @Test
    public void notReachable() throws IOException {
        mockWebServer.enqueue(new MockResponse.Builder().code(200)
                .body("haha").headersDelay(2, TimeUnit.SECONDS).build());
        String content =
                Files.readString(Paths.get("src","test", "resources","when_lint_no_errors.json"));
        JsonObject contentJson = ActionHelper.getYamlJson(content);
        YamlPipelineLinter linterWithTimeout =
                new YamlPipelineLinter(mockWebServer.url("/").toString(), "", 500);
        JsonObject result = linterWithTimeout.ciLint(contentJson);
        assertEquals(HttpStatus.SC_REQUEST_TIMEOUT, result.get(GITLAB_RESPONSE_STATUS).getAsInt());
    }

    @Test
    public void notAuthorized() throws IOException {
        String responseBody = "{\"message\":\"401 Unauthorized\"}";
        mockWebServer.enqueue(new MockResponse.Builder().code(401).body(responseBody).build());
        String content =
                Files.readString(Paths.get("src","test", "resources","when_lint_no_errors.json"));
        JsonObject contentJson = ActionHelper.getYamlJson(content);
        JsonObject result = pipelineLinter.ciLint(contentJson);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, result.get(GITLAB_RESPONSE_STATUS).getAsInt());
    }

    @Test
    void noJobErrors() throws IOException {
        JsonObject expectedGitlabResponse = JsonParser.parseString(
                "{\"valid\":true,\"errors\":[],\"warnings\":[],\"includes\":[],\"status\":\"valid\"}")
                .getAsJsonObject();
        mockWebServer.enqueue(new MockResponse.Builder().code(200).body(expectedGitlabResponse.toString()).build());
        String content =
                Files.readString(Paths.get("src","test", "resources","when_lint_no_errors.json"));
        JsonObject contentJson = ActionHelper.getYamlJson(content);
        JsonObject result = pipelineLinter.ciLint(contentJson);
        assertEquals(HttpStatus.SC_OK, result.get(GITLAB_RESPONSE_STATUS).getAsInt());
        assertEquals(expectedGitlabResponse, result.get("gitlabResponse"));
    }
}
