package org.ideplugins.ci_pipeline_lint.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.SystemInfo;
import org.ideplugins.ci_pipeline_lint.settings.PipelinePluginConfigurationState;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FeedbackActionHelper {
    final String ideVersion;
    final String pluginVersion;
    final String operatingSystem;
    final String jdkVersion;
    final String title;
    final String description;

    private static final String SYSTEM_INFO_TEMPLATE = """
            | Attribute                            | Value |
            |--------------------------------------|-------|
            | **OS**                               |   %s  |
            | **IDE**                              |   %s  |
            | **JDK**                              |   %s  |
            | **CI Pipeline Lint Plugin Version**  |   %s  |
            
            /label ~bug-report
            """;

    private static final String FEATURE_REQUEST_TEMPLATE = """
            ## Feature request
            
            ## Description
            
            
            /label ~feature-request
            """;

    private static final String BUG_TEMPLATE = """
## Summary
- [ ] Brief summary

## Steps to reproduce

1. - [ ] Steps to reproduce

## Actual results

1. - [ ] What does it happen?

## Attach or paste relevant plugin logs

Get relevant logs of the plugin by running this grep command:

```bash
grep -i "org.ideplugins.ci_pipeline_lint" ~/.cache/JetBrains/IdeaIC2023.3/log/idea.log
```

please
read [Directories used by the IDE to store settings caches plugins and logs](https://intellij-support.jetbrains.com/hc/en-us/articles/206544519-Directories-used-by-the-IDE-to-store-settings-caches-plugins-and-logs)
to find out the directory where to look for the log file according to your OS.

**Make sure these logs have no sensitive information.**

## Screenshots or screencast
Attach a screencast/screenshot to help reproduce the issue.

**Make sure the screenshot or screencast have no sensitive information.**
            """;

    private static final String ISSUES_URL =
            "https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/new?issue[title]=%s&issue[description]=%s";


    public FeedbackActionHelper(AnActionEvent actionEvent){
        title = "Bug Report".equals(actionEvent.getPresentation().getText())?
                "Plugin Bug Report" : "Plugin Feature Request";
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        operatingSystem = SystemInfo.getOsNameAndVersion() + "-" + SystemInfo.OS_ARCH;
        ideVersion = String.join(" ", applicationInfo.getVersionName(),
                applicationInfo.getFullVersion(), applicationInfo.getBuild().asString());
        jdkVersion = String.join(" ", System.getProperty("java.vm.name"),
                SystemInfo.JAVA_VERSION, SystemInfo.JAVA_RUNTIME_VERSION, SystemInfo.JAVA_VENDOR);
        PipelinePluginConfigurationState pluginSettings =
                ApplicationManager.getApplication().getService(PipelinePluginConfigurationState.class);
        pluginVersion = pluginSettings.getLastVersion();
        description = "Bug Report".equals(actionEvent.getPresentation().getText())? BUG_TEMPLATE +
                SYSTEM_INFO_TEMPLATE.formatted(operatingSystem, ideVersion, jdkVersion, pluginVersion) :
                FEATURE_REQUEST_TEMPLATE;
    }

    private String encode(final String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    public String getEncodedUrl() {
        return String.format(ISSUES_URL,encode(title), encode(description));
    }
}
