package org.ideplugins.plugin.linter;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;

public interface Constants {
    String GITLAB_URL = "https://gitlab.com/api/v4/ci/lint";
    String NOTIFICATION_TITLE = "Gitlab Pipeline Lint";
    String GROUP_DISPLAY_ID = "org.ideplugins.pipeline-lint";
    String GITLAB_CI_YML = ".gitlab-ci.yml";
    String GITLAB_RESPONSE_BODY = "gitlabResponse";
    String GITLAB_RESPONSE_STATUS = "httpStatus";

    String PLUGIN_ID = "org.ideplugins.gitlab-yaml-pipeline-lint";
    CredentialAttributes CREDENTIAL_ATTRIBUTES = new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("ci-pipeline-lint", "gitlab-token")
    );
}
