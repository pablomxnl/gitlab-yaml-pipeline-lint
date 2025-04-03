# CI Pipeline Lint Plugin Documentation v %plugin_version%

Plugin for JetBrains IDE’s that uses [Gitlab CI Lint API](https://docs.gitlab.com/ee/api/lint.html) to validate the content of a project gitlab-ci.yml pipeline file

## Installation
<tabs>
    <tab title="From Jetbrains Plugin Marketplace">
    <kbd>Settings</kbd> (or <kbd>Preferences</kbd> if using mac) -> <kbd>Plugins</kbd> -> <kbd>Marketplace</kbd> Search for CI Pipeline Lint -> Click <control>Install</control>
    <img src="install_plugin.png" border-effect="line" alt="Install plugin" /> 
</tab>
    <tab title="From Gitlab Releases">
To install, grab a zip file from 
<a href="https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/releases">gitlab releases</a> and then install it by going to
<kbd>Settings</kbd> (or <kbd>Preferences</kbd> if using mac) -> <kbd>Plugins</kbd> -> <kbd>⚙</kbd> -> <kbd>Install Plugin from Disk...</kbd>
<img src="install_plugin_from_disk.png" border-effect="line" alt="Install plugin from disk" /> 
Then select the zip file just downloaded from gitlab releases.
    </tab>
</tabs>

## Configuration

The plugin requires a Gitlab token and a Project ID to work with the Gitlab CI Lint API.
To set up this configuration go to 

<kbd>Settings</kbd> (or <kbd>Preferences</kbd> if using mac) -> <kbd>Tools</kbd> -> <kbd>CI Pipeline Lint</kbd> 

![Plugin settings](plugin_settings.png){border-effect="line"}

1. Gitlab host: default to https://gitlab.com , change it if using a private or self-hosted instance
2. Gitlab Project ID: To obtain the Project ID go to a gitlab repository you own and at the top, below the project name the Project ID is displayed, there is also a copy button next to the Project ID. Gitlab 16.0 [deprecated](https://docs.gitlab.com/ee/api/lint.html#validate-the-ci-yaml-configuration-deprecated) the global lint endpoint, and now is per project, that's why a Project ID is now required.
3. Access token: create a Gitlab personal access token with api access, the link on the label will open the browser at [https://gitlab.com/-/user_settings/personal_access_tokens](https://gitlab.com/-/user_settings/personal_access_tokens) to create a token. 

Alternatively, whenever the plugin is invoked with incomplete configuration, the settings can be entered by clicking on the notification link:

![Plugin settings from notification](plugin_settings_when_not_configured.png){border-effect="line"}


## Usage

To validate the gitlab pipeline file, do one of the following:

* Using the global tools menu action: <kbd>Tools</kbd> -> <kbd>CI Pipeline Lint</kbd>

![Tools menu action](usage_tools_menu.png){ border-effect="line"}

* Using the popup action in the editor window
 
![Editor popup action](usage_editor_context_menu.png){ border-effect="line"}

* Using the popup action on the project view by selecting the <path>.gitlab-ci.yml</path> file

![Project view popup action](usage_project_context_menu.png){border-effect="line"}
 

## Results

The results of the call to the Gitlab CI Lint API are displayed in 

* The plugin tool window
* On the problem view (if there are errors or warnings)
* As annotations on the editor if the file <path>.gitlab-ci.yaml</path> is loaded.

### Results in plugin tool window

<img src="results_toolwindow_annotator.png" alt="Results in plugin window and editor annotator" border-effect="line" />

The plugin tool window will show the errors and warnings returned by the Gitlab CI Lint API , if the pipeline file is in the editor the results
are annotated (those little gutters in red/yellow ).

### Results in problem view

<img src="results_problemview_annotator.png" alt="Results in problem view" border-effect="line" />

The errors and warnings returned by the Gitlab CI Lint API can also be visualized on the problem view clicking on them will navigate to the line 
where the job definition with error or warning is (unfortunately the Gitlab API doesn't provide a line number so the whole job definition is annotated).

### Results of a valid pipeline

<img src="results_toolwindow_no_errors_or_warnings.png" alt="Valid pipeline results" border-effect="line"/>

When no errors/warnings are returned a simple success message is displayed in the plugin tool window.

> **NOTE:**
>
> It means the yaml int the pipeline file is correct, but there are no guarantees that the pipeline will succeed
>
{style="note"}
