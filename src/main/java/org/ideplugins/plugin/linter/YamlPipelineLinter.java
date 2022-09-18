package org.ideplugins.plugin.linter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;
import org.ideplugins.plugin.settings.YamlPipelineLintSettingsState;

import java.io.IOException;
import java.util.Objects;

import static org.ideplugins.plugin.actions.ActionHelper.displayNotification;

public class YamlPipelineLinter implements Constants {

    private static final Logger LOGGER = Logger.getInstance(YamlPipelineLinter.class);
    private static final OkHttpClient client = new OkHttpClient();

    public static JsonObject ciLint(JsonObject yamlJson) {
        String result = "";
        JsonObject jsonObject = new JsonObject();
        Request request = createPostRequest(yamlJson);
        try (Response response = client.newCall(request).execute()) {
            result = response.body().string();
            jsonObject.addProperty(GITLAB_RESPONSE_STATUS, response.code());
        } catch (IOException ioe) {
            LOGGER.error("Not able to reach gitlab", ioe);
        }
        try {
            JsonElement jsonElement = JsonParser.parseString(result);
            jsonObject.add(GITLAB_RESPONSE_BODY, jsonElement);
        } catch (JsonSyntaxException jse) {
            jsonObject.addProperty(GITLAB_RESPONSE_BODY, result);
        }
        return jsonObject;
    }

    private static Request createPostRequest(JsonObject yaml) {
//        yaml.addProperty("include_jobs", true);
        RequestBody body = RequestBody.create(yaml.toString(), MediaType.get("application/json; charset=utf-8"));
        Credentials credentials = PasswordSafe.getInstance().get(Constants.CREDENTIAL_ATTRIBUTES);
        String token = Objects.requireNonNull(credentials).getPasswordAsString();
        return new Request.Builder().addHeader("PRIVATE-TOKEN", Objects.requireNonNull(token))
                .addHeader("Content-ype", "application/json")
                .url(ApplicationManager.getApplication().getService(YamlPipelineLintSettingsState.class)
                        .gitlabEndpoint).post(body).build();
    }

    public static boolean checkGitlabToken() {
        Credentials credentials = PasswordSafe.getInstance().get(CREDENTIAL_ATTRIBUTES);
        if (credentials == null) {
            displayNotification(NotificationType.WARNING, "Please setup your Gitlab token");
            return false;
        }
        return true;
    }
}
