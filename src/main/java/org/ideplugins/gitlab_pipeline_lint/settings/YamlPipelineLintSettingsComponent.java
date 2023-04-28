package org.ideplugins.gitlab_pipeline_lint.settings;


import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import org.apache.commons.lang.StringUtils;
import org.ideplugins.gitlab_pipeline_lint.linter.Constants;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static org.ideplugins.gitlab_pipeline_lint.linter.Constants.GITLAB_HOST;

public class YamlPipelineLintSettingsComponent {

    private final JPanel myMainPanel;
    private final JBTextField gitlabEndpoint = new JBTextField();

    private final JBTextField gitlabHost = new JBTextField();

    private final JBPasswordField gitLabToken = new JBPasswordField();

    private final JBTextField gitlabProjectID = new JBTextField();
    private final DocumentListener changeListener = createFieldsListener();


    private DocumentListener createFieldsListener() {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                if (StringUtils.isNumeric(gitlabProjectID.getText())){
                    gitlabEndpoint.setText(String.format(Constants.GITLAB_URL, gitlabHost.getText(), gitlabProjectID.getText()));
                }
            }
        };
    }



    @NotNull
    private JBLabel createProjectIDScreenshot() {
        JBLabel screenshot = new JBLabel();
        screenshot.setIcon(Constants.gitlabProjectIdScreenshot);
        return screenshot;
    }

    public YamlPipelineLintSettingsComponent() {
        JBLabel screenshot = createProjectIDScreenshot();
        setChangeListeners();
        BrowserLink tokenLink = new BrowserLink("Enter access token", String.format("https://%s/-/profile/personal_access_tokens",
                GITLAB_HOST));
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Enter Gitlab host"), gitlabHost, 1, false)
                .addLabeledComponent(new JBLabel("Enter project ID"), gitlabProjectID, 2, false)
                .addLabeledComponent("How to get Project ID", screenshot,3,false)
                .addLabeledComponent(tokenLink, gitLabToken, 4, false)
                .addLabeledComponent(new JBLabel("Endpoint to be used"), gitlabEndpoint, 5, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        myMainPanel.setToolTipText("To get the gitlab Project ID\n, go to your repository and click the copy button in the screenshot.");
    }

    private void setChangeListeners() {
        gitlabEndpoint.setEditable(false);
        gitlabProjectID.getDocument().addDocumentListener(changeListener);
        gitlabHost.getDocument().addDocumentListener(changeListener);
        gitlabProjectID.setToolTipText("To get the gitlab Project ID\n, go to your repository and click the copy button in the screenshot.");
    }

    public JComponent getPreferredFocusedComponent() {
        return gitlabHost;
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

    public String getGitlabProjectID() {
        return gitlabProjectID.getText();
    }

    public void setGitlabProjectID(@NotNull String newText) {
        gitlabProjectID.setText(newText);
    }

    public void setGitlabHost(String host) {
        gitlabHost.setText(host);
    }

    public String getGitlabHost() {
        return gitlabHost.getText();
    }
}
