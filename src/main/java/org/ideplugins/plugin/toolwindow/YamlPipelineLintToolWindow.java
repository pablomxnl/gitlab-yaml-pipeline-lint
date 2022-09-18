package org.ideplugins.plugin.toolwindow;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

public class YamlPipelineLintToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        Content content = toolWindow.getContentManager().getFactory().createContent(consoleView.getComponent(),
                "Gitlab Pipeline Lint Results", true);
        content.setDisplayName("Lint Results");
        toolWindow.setTitle("Gitlab Pipeline Lint Results");
        toolWindow.setType(ToolWindowType.DOCKED, null);
        toolWindow.getContentManager().addContent(content);

    }
}
