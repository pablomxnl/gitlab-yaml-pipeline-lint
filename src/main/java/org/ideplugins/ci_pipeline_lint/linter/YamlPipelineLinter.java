package org.ideplugins.ci_pipeline_lint.linter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.Optional;
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
        String result="";
        JsonObject jsonObject = new JsonObject();
        Request request = createPostRequest(yamlJson);
        String host = request.url().host();
        try (Response response = client.newCall(request).execute()) {
            Optional<ResponseBody> body = Optional.ofNullable(response.body());
            if (body.isPresent()){
                result = body.get().string();
            }
            jsonObject.addProperty(GITLAB_RESPONSE_STATUS, response.code());
        } catch (IOException ioe) {
            LOGGER.info(String.format("Not able to reach gitlab at %s", host), ioe);
            jsonObject.addProperty(GITLAB_RESPONSE_STATUS, HttpStatus.SC_REQUEST_TIMEOUT);
            JsonObject error = new JsonObject();
            error.addProperty("status", "UNKNOWN");
            error.addProperty("exceptionMessage", ioe.getMessage());
            result = error.toString();
        }
        try {
            JsonElement jsonElement = JsonParser.parseString(result);
            jsonObject.add(GITLAB_RESPONSE_BODY, jsonElement);
        } catch (JsonSyntaxException jse) {
            jsonObject.addProperty(GITLAB_RESPONSE_BODY, result);
        }
        return jsonObject;
    }

    private Request createPostRequest(JsonObject yaml) {
//        yaml.addProperty("include_jobs", true);
        RequestBody body = RequestBody.create(yaml.toString(), MediaType.get("application/json; charset=utf-8"));
        return new Request.Builder().addHeader("PRIVATE-TOKEN", token)
                .addHeader("Content-ype", "application/json")
                .url(url).post(body).build();
    }

}
