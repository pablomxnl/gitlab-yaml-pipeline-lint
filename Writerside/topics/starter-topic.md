# Gitlab Pipeline Lint Plugin Documentation

<!--Writerside adds this topic when you create a new documentation project.
You can use it as a sandbox to play with Writerside features, and remove it from the TOC when you don't need it anymore.-->
Plugin for JetBrains IDE’s that uses Gitlab CI Lint API to validate the content of a project gitlab-ci.yml pipeline file

## Installation
<tabs>
    <tab title="From Jetbrains Plugin Marketplace">
    <kbd>Settings</kbd> (or <kbd>Preferences</kbd> if using mac) -> <kbd>Plugins</kbd> -> <kbd>Marketplace</kbd> Search for Gitlab Pipeline Lint -> Click <control>Install</control>

</tab>
    <tab title="From Gitlab Releases">
To install, grab a zip file from 
<a href="https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/releases">gitlab releases</a> and then install it by going to
<kbd>Settings</kbd> (or <kbd>Preferences</kbd> if using mac) -> <kbd>Plugins</kbd> -> <kbd>⚙</kbd> -> <kbd>Install Plugin from Disk...</kbd>
    </tab>
</tabs>



## Configuration

The plugin requires a Gitlab token and a Project ID to work with the Gitlab Pipeline Lint API.
To setup this configuration go to 

<kbd>Settings</kbd> (or <kbd>Preferences</kbd> if using mac) -> <kbd>Tools</kbd> -> <kbd>Gitlab Pipeline Lint</kbd> 

<img src="plugin_settings.png" alt="Plugin settings" border-effect="line"/>

1. Gitlab host: default to https://gitlab.com , change it if using a private or self hosted instance
2. Gitlab Project ID: To obtain the Project ID go to a gitlab repository you own and at the top, below the project name the Project ID is displayed, there is also a copy button next to the Project ID. Gitlab 16.0 deprecated the global lint endpoint, and now is per project, that's why a Project ID is now required.
3. Access token: create a Gitlab personal access token with api access, the link on the label will open the browser at <a href="https://gitlab.com/-/profile/personal_access_tokens">https://gitlab.com/-/profile/personal_access_tokens</a> to obtain a token if needed. 

Alternatively, whenever the plugin is invoked with incomplete configuration, the settings can be entered by clicking on the notification link:

<img src="plugin_settings_when_not_configured.png" alt="Plugin settings from notification" border-effect="line" />


## Usage

To validate the gitlab pipeline file, do one of the following:

* Using the global tools menu action: <kbd>Tools</kbd> -> <kbd>Gitlab Pipeline Lint</kbd>
  <img src="usage_tools_menu.png" alt="Tools menu action" border-effect="line"/>
* Using the popup action in the editor window
  <img src="usage_editor_context_menu.png" alt="Editor popup action" border-effect="line"/>
* Using the popup action on the project view by selecting the <path>.gitlab-ci.yml</path> file
  <img src="usage_project_context_menu.png" alt="Project view popup action" border-effect="line"/>

## Results

The results of the call to the Gitlab Pipeline Lint API are displayed in 

* The plugin tool window
* On the problem view (if there are errors or warnings)
* As annotations on the editor if the file <path>.gitlab-ci.yaml</path> is loaded.

### Results in plugin tool window

<img src="results_toolwindow_annotator.png" alt="Results in plugin window and editor annotator" border-effect="line" />

The plugin tool window will show the errors and warnings returned by the Gitlap Pipeline Lint API , if the pipeline file is in the editor the results
are annotated (those little gutters in red/yellow ).

### Results in problem view

<img src="results_problemview_annotator.png" alt="Results in problem view" border-effect="line" />

The errors and warnings returned by the Gitlap Pipeline Lint API can also be visualized on the problem view clicking on them will navigate to the line 
where the job definition with error or warning is (unfortunately the Gitlab API doesn't provide a line number so the whole job definition is annotated).

### Results of a valid pipeline

<img src="results_toolwindow_no_errors_or_warnings.png" alt="Valid pipeline results"/>

When no errors/warnings are returned a simple success message is displayed in the plugin tool window.

> **NOTE:**
>
> It means the yaml pipeline is correct, but not there are no guarantees the pipeline will succeed
>
{style="note"}


## Feedback 
Please report any issues or feature requests at
<a href="hhttps://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint/-/issues">Gitlab Issues</a> (a gitlab.com account is required).
