package org.ideplugins.ci_pipeline_lint.completion;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Conditions;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class GitLabCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static final List<LookupElement> variables = buildCompletionItemList();


    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {

        PsiElement psi = parameters.getPosition().getOriginalElement();
        AutoPopupController controller = AutoPopupController.getInstance(psi.getProject());
        if (psi.textContains('$') && psi.textContains('"')){
            result.withPrefixMatcher(PrefixMatcher.ALWAYS_TRUE).addAllElements(variables); //
        } else {
            result.addAllElements(variables);
        }

        controller.scheduleAutoPopup(parameters.getEditor(), CompletionType.BASIC, Conditions.alwaysTrue());
//        controller.autoPopupMemberLookup(parameters.getEditor(), CompletionType.BASIC, Conditions.alwaysTrue());
    }

    private static List<LookupElement> buildCompletionItemList(){
        List<GitlabVariable> gitlabVariables = loadVariablesData();
        return gitlabVariables.stream().map(x -> LookupElementBuilder.create(x.getName()).withTypeText(x.getDescription())).collect(Collectors.toList());
    }



    private static List<GitlabVariable> loadVariablesData() {
        String DATA_FILE = "gitlab-variables.json";
        InputStream in = GitLabCompletionProvider.class.getClassLoader().getResourceAsStream(DATA_FILE);
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        return gson.fromJson(reader, new TypeToken<List<GitlabVariable>>(){}.getType());
    }

}
