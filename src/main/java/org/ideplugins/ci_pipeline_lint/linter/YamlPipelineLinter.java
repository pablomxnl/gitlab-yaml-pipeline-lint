package org.ideplugins.ci_pipeline_lint.linter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class YamlPipelineLinter implements Constants {

    private static final Logger LOGGER = Logger.getInstance(YamlPipelineLinter.class);

    private final OkHttpClient client;

    private final String url;
    private final String token;

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
                    Optional<ResponseBody> body = Optional.ofNullable(response.body());
                    String gitlabResponse = "";
                    if (body.isPresent()) {
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
        return new Request.Builder().addHeader("PRIVATE-TOKEN", token)
                .addHeader("Content-ype", "application/json")
                .url(url).post(body).build();
    }

}
