What&apos;s new
=====
This document provides an overview of the changes by release.

0.0.15
------

- Added toolbar with feedback buttons to the plugin tool window
- Gitlab Variables autocomplete on CI/CD catalog component files matching patterns
    * `.gitlab-ci-*.yml`
    * `.gitlab-ci-*.yaml`
    * `gitlab-ci-*.yml`
    * `gitlab-ci-*.yaml`
- Ignore [error reports](https://plugins.jetbrains.com/plugin/19972-ci-pipeline-lint/docs/feedback.html#automatic-error-reporting) from outdated plugin versions.


0.0.14
------

- Update to support EAP 251
- Fixes [\#13](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/issues/13) Conflicting component name PluginSettingsState reported by plugin user through sentry error reporter

0.0.13
------

- Closes [\#12](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/12) Rename plugin to address Unauthorized Use of GitLab Intellectual Property email
- Fixes [\#11](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/11) IllegalArgumentException reported by plugin user through sentry error reporter

0.0.12
------

- Update to support EAP 243
- Fixes [\#9](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/9) NPE reported by plugin user through sentry error reporter
- Added Show token checkbox to plugin settings dialog [\#10](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/10)

0.0.11
------
- Update to new intellij platform gradle plugin / java 21 to support EAP 242
- Fixes [\#7](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/7) IndexNotReadyException reported by plugin user

0.0.10
------

- Fixes [\#5](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/5) ClassCastException reported by plugin user through sentry error reporter
- Fixes [\#6](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/6) Update pluginUntilBuild to support 2024.x versions

0.0.9
------

- Fixes [\#4](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/4) Gitlab CI autocomplete popup shows when editing docker-compose file
- Fixes [\#3](https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues/3) Slow operations are prohibited on EDT

0.0.8
------

- Support for 2023.3 (EAP)

0.0.7
------

- Enable settings sync and update pluginUntilBuild to support 2023.2 EAP
- Java 17 now required

0.0.6
------

- Adjust to use new pipeline lint endpoint per project as old global endpoint is removed in Gitlab 16.0

0.0.5
------

- Add autocomplete for gitlab variables

0.0.4
------

- Annotating the results in the problem view / editor
- Improve error reporting to include warnings
- Add uncaught error reporting
- Plugin update notification begging for rate/review 🤣

0.0.3
------

- Minor changes displaying lint results

0.0.2
------

- Add link action to jump to plugin settings to add gitlab token

0.0.1
------

- Initial version
