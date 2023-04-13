package org.ideplugins.gitlab_pipeline_lint.gitlab;

import com.google.gson.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class FetchGitlabVariables {

    private static final Logger LOGGER = Logger.getLogger(FetchGitlabVariables.class.getName());

    public static void main(final String[] args) {
        OkHttpClient client = new OkHttpClient();
        String filePath = args[0];
        String url = "https://gitlab.com/gitlab-org/gitlab/-/raw/master/doc/ci/variables/predefined_variables.md";
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String content = response.body().string();
                JsonArray jsonArray = new JsonArray();

                content.lines().filter(line -> line.startsWith("| `")).forEach(line -> {
                    String[] values = line.split("\\|");
                    JsonObject object = new JsonObject();
                    object.add("Variable",
                            new JsonPrimitive(values[1].strip().replaceAll("`", "")));
                    object.add("GitLab", new JsonPrimitive(values[2].strip()));
                    object.add("Runner", new JsonPrimitive(values[3].strip()));
                    object.add("Description", new JsonPrimitive(values[4].stripLeading()));
                    jsonArray.add(object);
                });
                Files.writeString(Path.of(filePath), jsonArray.toString());
            }
        } catch (IOException ioe) {
            LOGGER.severe("Not able to reach gitlab" + ioe.getMessage());
        }
    }
}
