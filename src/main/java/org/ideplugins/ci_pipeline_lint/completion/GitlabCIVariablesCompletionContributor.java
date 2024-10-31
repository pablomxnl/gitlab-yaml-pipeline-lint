package org.ideplugins.ci_pipeline_lint.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class GitlabCIVariablesCompletionContributor extends CompletionContributor {

    public GitlabCIVariablesCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE)
                        .inFile(PlatformPatterns.psiFile().withName(".gitlab-ci.yml")),
                new GitLabCompletionProvider()
        );

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE)
                        .inFile(PlatformPatterns.psiFile().withName(".gitlab-ci.yml"))
                                .and(psiElement().withElementType(YAMLElementTypes.TEXT_SCALAR_ITEMS)
                                .and(psiElement().withText("$"))
                                .and(psiElement().withParent(psiElement(YAMLElementTypes.SCALAR_PLAIN_VALUE)))
                                .and(psiElement().withSuperParent(2, psiElement(YAMLSequenceItem.class))))
                ,
                new GitLabCompletionProvider()
        );
    }
}
