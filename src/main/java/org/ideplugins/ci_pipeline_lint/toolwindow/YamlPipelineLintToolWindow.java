package org.ideplugins.ci_pipeline_lint.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

public class YamlPipelineLintToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        YamlPipelineLintToolWindowContent ciLintToolWindowContent =
                new YamlPipelineLintToolWindowContent(project);
        Content content = toolWindow.getContentManager().getFactory()
                .createContent(ciLintToolWindowContent.getComponent(),
                "CI Pipeline Lint Results", true);
        content.setDisplayName("Lint Results");
        toolWindow.setTitle("CI Pipeline Lint Results");
        toolWindow.setType(ToolWindowType.DOCKED, null);
        content.setDisposer(ciLintToolWindowContent);
        ContentManager manager = toolWindow.getContentManager();
        manager.addContent(content);
    }
}
