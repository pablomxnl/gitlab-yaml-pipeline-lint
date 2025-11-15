package org.ideplugins.ci_pipeline_lint.completion;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GitLabCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static final Logger LOGGER = Logger.getInstance(GitLabCompletionProvider.class);
    private static final List<LookupElement> variables = buildCompletionItemList();


    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
            result.addAllElements(variables);
    }

    private static List<LookupElement> buildCompletionItemList(){
        List<GitlabVariable> gitlabVariables = loadVariablesData();
        return gitlabVariables.stream().map(x ->
                LookupElementBuilder.create(x.getName()).withTypeText(x.getDescription())).collect(Collectors.toList());
    }

    private static List<GitlabVariable> loadVariablesData() {
        String DATA_FILE = "gitlab-variables.json";
        try (InputStream in = GitLabCompletionProvider.class.getClassLoader().getResourceAsStream(DATA_FILE)) {
            Gson gson = new Gson();
            Reader reader = new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8);
            return gson.fromJson(reader, new TypeToken<List<GitlabVariable>>(){}.getType());
        } catch (IOException e) {
            LOGGER.error("Unable to load gitlab variables json file from plugin classpath", e);
            throw new RuntimeException(e);
        }
    }

}
