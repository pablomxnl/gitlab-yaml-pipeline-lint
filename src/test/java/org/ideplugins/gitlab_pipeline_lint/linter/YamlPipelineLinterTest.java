package org.ideplugins.gitlab_pipeline_lint.linter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.HttpStatus;
import org.ideplugins.gitlab_pipeline_lint.actions.ActionHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class YamlPipelineLinterTest implements Constants {

    private MockWebServer mockWebServer;
    private YamlPipelineLinter pipelineLinter;

    @BeforeEach
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        pipelineLinter = new YamlPipelineLinter(mockWebServer.url("/").toString(), "");
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }


    @Test
    public void notReachable() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("haha").setHeadersDelay(2, TimeUnit.SECONDS));
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
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(responseBody));
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
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(expectedGitlabResponse.toString()));
        String content =
                Files.readString(Paths.get("src","test", "resources","when_lint_no_errors.json"));
        JsonObject contentJson = ActionHelper.getYamlJson(content);
        JsonObject result = pipelineLinter.ciLint(contentJson);
        assertEquals(HttpStatus.SC_OK, result.get(GITLAB_RESPONSE_STATUS).getAsInt());
        assertEquals(expectedGitlabResponse, result.get("gitlabResponse"));
    }
}
