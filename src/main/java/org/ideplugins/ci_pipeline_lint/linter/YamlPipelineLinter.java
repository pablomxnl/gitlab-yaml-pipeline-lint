package org.ideplugins.ci_pipeline_lint.linter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import okhttp3.*;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


import org.ideplugins.ci_pipeline_lint.settings.ProjectGitSettingsState;
import org.ideplugins.ci_pipeline_lint.settings.YamlPipelineLintSettingsState;
import org.ideplugins.ci_pipeline_lint.service.PasswordSafeService;

public class YamlPipelineLinter implements Constants {

    private static final Logger LOGGER = Logger.getInstance(YamlPipelineLinter.class);

    private final OkHttpClient client;

    private final String url;
    private final String token;

    // Backwards-compatible constructors (used by tests or other callers)
    public YamlPipelineLinter(String url, String token) {
        this.url = url;
        this.token = token;
        client = new OkHttpClient.Builder().build();
    }

    YamlPipelineLinter(String url, String token, long timeout) {
        this.url = url;
        this.token = token;
        client = new OkHttpClient.Builder().callTimeout(timeout, TimeUnit.MILLISECONDS).build();
    }

    // New constructors: build URL and token from application/project settings
    public YamlPipelineLinter(Project project) {
        this(project, 0L);
    }

    public YamlPipelineLinter(Project project, long timeout) {
        YamlPipelineLintSettingsState yamlState = ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class);
        ProjectGitSettingsState projectState = project.getService(ProjectGitSettingsState.class);

        // token is stored in PasswordSafe; retrieve from PasswordSafeService
        this.token = PasswordSafeService.retrieveToken();

        String projectId = projectState != null && projectState.projectId != null ? projectState.projectId : "";
        String host = yamlState != null && yamlState.gitlabHost != null ? yamlState.gitlabHost : Constants.GITLAB_HOST;
        this.url = String.format(Constants.GITLAB_URL, host, projectId);
        client = timeout > 0 ? new OkHttpClient.Builder().callTimeout(timeout, TimeUnit.MILLISECONDS).build()
                : new OkHttpClient.Builder().build();
    }

    public JsonObject ciLint(JsonObject yamlJson) {
        try {
            return ciLintPooled(yamlJson).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<JsonObject> ciLintPooled(JsonObject yamlJson) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Request request = createPostRequest(yamlJson);
        LOGGER.info(String.format("Posting pipeline to %s", request.url()));
        callCILintEndpoint(request)
                .thenAccept(response -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty(GITLAB_RESPONSE_STATUS, response.code());
                    Optional<ResponseBody> body = Optional.of(response.body());
                    String gitlabResponse = "";
                    try {
                        gitlabResponse = body.get().string();
                        LOGGER.info(String.format("Received response %s", gitlabResponse));
                        JsonElement jsonElement = JsonParser.parseString(gitlabResponse);
                        jsonObject.add(GITLAB_RESPONSE_BODY, jsonElement);
                        future.complete(jsonObject);
                    } catch (JsonSyntaxException jse) {
                        jsonObject.addProperty(GITLAB_RESPONSE_BODY, gitlabResponse);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                })
                .exceptionally(ex -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty(GITLAB_RESPONSE_STATUS, HttpStatus.SC_REQUEST_TIMEOUT);
                    JsonObject error = new JsonObject();
                    error.addProperty("status", "UNKNOWN");
                    error.addProperty("exceptionMessage", ex.getMessage());
                    jsonObject.add(GITLAB_RESPONSE_BODY, error);
                    future.complete(jsonObject);
                    return null;
                });
        return future;
    }

    private CompletableFuture<Response> callCILintEndpoint(Request request) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException ioe) {
                future.completeExceptionally(ioe);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                future.complete(response);
            }
        });
        return future;
    }

    private Request createPostRequest(JsonObject yaml) {
//        yaml.addProperty("include_jobs", true);
        RequestBody body = RequestBody.create(yaml.toString(), MediaType.get("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (token != null && !token.isBlank()) {
            builder.addHeader("PRIVATE-TOKEN", token);
        }
        builder.addHeader("Content-Type", "application/json");
        return builder.build();
    }

}
