package org.ideplugins.ci_pipeline_lint.toolwindow;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

public class YamlPipelineLintToolWindowContent extends SimpleToolWindowPanel implements Disposable {

    private ConsoleView consoleView;
    private Project project;

    public YamlPipelineLintToolWindowContent(@NotNull Project project){
        super(true);
        this.project = project;
        ActionToolbar actionToolbar = createToolbar();
        consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        add(consoleView.getComponent());
        actionToolbar.setTargetComponent(null);
        setToolbar(actionToolbar.getComponent());
    }

    public ConsoleView getConsoleView() {
        return consoleView;
    }


    @Override
    public void dispose() {
        consoleView.clear();
        removeAll();
        Disposer.dispose(consoleView);
        project = null;
        consoleView = null;
    }

    private ActionToolbar createToolbar() {
        DefaultActionGroup vale = new DefaultActionGroup();
        vale.add(ActionManager.getInstance()
                .getAction("org.ideplugins.vale_cli_plugin.actions.CIPipelineLintFeedbackAction"));
        vale.add(ActionManager.getInstance()
                .getAction("org.ideplugins.vale_cli_plugin.actions.CIPipelineLintBugReportAction"));
        return ActionManager.getInstance()
                .createActionToolbar(ActionPlaces.TOOLBAR, vale, true);
    }
}
