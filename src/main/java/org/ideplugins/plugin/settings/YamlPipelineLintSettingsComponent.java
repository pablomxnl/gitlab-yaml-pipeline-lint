package org.ideplugins.plugin.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class YamlPipelineLintSettingsComponent {

    private final JPanel myMainPanel;
    private final JBTextField gitlabEndpoint = new JBTextField();

    private final JBPasswordField gitLabToken = new JBPasswordField();

    public YamlPipelineLintSettingsComponent() {
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Enter Gitlab endpoint"), gitlabEndpoint, 1, false)
                .addLabeledComponent(new JBLabel("Enter Gitlab token"), gitLabToken, 2, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JComponent getPreferredFocusedComponent() {
        return gitlabEndpoint;
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public void setGitlabEndpoint(@NotNull String newText) {
        gitlabEndpoint.setText(newText);
    }

    public void setGitlabToken(@NotNull String newText) {
        gitLabToken.setText(newText);
    }

    @NotNull
    public String getGitlabEndpoint() {
        return gitlabEndpoint.getText();
    }

    @NotNull
    public String getGitlabToken() {
        return new String(gitLabToken.getPassword());
    }

}
