package org.ideplugins.ci_pipeline_lint.settings;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.event.ItemEvent;

import static org.ideplugins.ci_pipeline_lint.linter.Constants.GITLAB_HOST;

public class YamlPipelineLintSettingsComponent {

    private final JPanel myMainPanel;
    private final JBTextField gitlabHost = new JBTextField();

    private final JBPasswordField gitLabToken = new JBPasswordField();
    private final JBCheckBox showToken = new JBCheckBox("Show token");

    private final JBTextField gitlabProjectID = new JBTextField();
    private final DocumentListener changeListener = createFieldsListener();


    private DocumentListener createFieldsListener() {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                // No-op: endpoint is computed at runtime per-project; do not show or compute it in global UI.
            }
        };
    }



    public YamlPipelineLintSettingsComponent() {
        // Do not expose project ID or autodetect in the global settings UI. Project-level detection
        // will run on project startup and store the detected values in a project-local service.
        setChangeListeners();
        BrowserLink tokenLink = new BrowserLink("Enter access token", String.format("https://%s/-/profile/personal_access_tokens",
                GITLAB_HOST));

        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Enter Gitlab host"), gitlabHost, 1, false)
                .addLabeledComponent(tokenLink, gitLabToken, 2, false)
                .addLabeledComponent("", showToken, 3, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        myMainPanel.setToolTipText("Configure GitLab host and token. Project ID and remote are detected per-project.");
    }

    private void setChangeListeners() {
        // project ID is not edited in the global settings UI
        gitlabHost.getDocument().addDocumentListener(changeListener);
        char defaultEcho = gitLabToken.getEchoChar();
        showToken.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED){
               gitLabToken.setEchoChar((char)0);
            } else {
               gitLabToken.setEchoChar(defaultEcho);
            }
        });

        gitlabProjectID.setToolTipText("Project ID is detected per-project on startup. If detection fails you will be prompted to enter the project ID.");
    }

    public JComponent getPreferredFocusedComponent() {
        return gitlabHost;
    }

    public JPanel getPanel() {
        return myMainPanel;
    }


    public void setGitlabToken(@NotNull String newText) {
        gitLabToken.setText(newText);
    }

    @NotNull
    public String getGitlabToken() {
        return new String(gitLabToken.getPassword());
    }

    public void setGitlabHost(String host) {
        gitlabHost.setText(host);
    }

    public String getGitlabHost() {
        return gitlabHost.getText();
    }


}
