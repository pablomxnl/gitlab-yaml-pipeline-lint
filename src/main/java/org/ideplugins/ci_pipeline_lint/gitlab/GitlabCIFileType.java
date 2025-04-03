package org.ideplugins.ci_pipeline_lint.gitlab;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.ideplugins.ci_pipeline_lint.icons.Icons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;

import javax.swing.*;

public class GitlabCIFileType extends LanguageFileType {

    private static GitlabCIFileType INSTANCE = new GitlabCIFileType();

    protected GitlabCIFileType() {
        super(YAMLLanguage.INSTANCE, true);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "Gitlab CI/CD Configuration";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Gitlab CI/CD Configuration";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "";
    }

    @Override
    public Icon getIcon() {
        return Icons.FileType;
    }

}
