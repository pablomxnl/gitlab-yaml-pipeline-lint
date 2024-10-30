package org.ideplugins.ci_pipeline_lint.linter;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface Constants {
    String GITLAB_URL = "https://%s/api/v4/projects/%s/ci/lint";
    String GITLAB_HOST = "gitlab.com";
    String NOTIFICATION_TITLE = "Gitlab Pipeline Lint";
    String GROUP_DISPLAY_ID = "org.ideplugins.pipeline-lint";
    String GITLAB_CI_YML = ".gitlab-ci.yml";
    String GITLAB_RESPONSE_BODY = "gitlabResponse";
    String GITLAB_RESPONSE_STATUS = "httpStatus";

    String PLUGIN_ID = "org.ideplugins.gitlab-yaml-pipeline-lint";
    CredentialAttributes CREDENTIAL_ATTRIBUTES = new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("pipeline-ci-lint", "gitlab-token")
    );

    Icon gitlabProjectIdScreenshot = IconLoader.getIcon("/icons/gitlabProjectID.png", Constants.class);
    String UPDATE_NOTIFICATION_TITLE = "Pipeline Lint plugin has been updated!!";
    String JB_MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/19972-gitlab-pipeline-lint/reviews";
    String UPDATE_NOTIFICATION_BODY =
            "Useful? Please rate / review";
    String NOTIFICATION_GROUP = "ci-pipeline-lint-plugin-update";

}
