package org.ideplugins.ci_pipeline_lint.settings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitlabRemoteParser {
    private static final Pattern REMOTE_PATTERN = Pattern.compile(
            "(?:ssh://)?(?:https?://|git://)?(?:[^@\\s]+@)?([^/:]+)(?::\\d+)?[/:](.+?)(\\.git)?$"
    );

    public static class RemoteInfo {
        public final String host;
        public final String projectPath;

        public RemoteInfo(String host, String projectPath) {
            this.host = host;
            this.projectPath = projectPath;
        }
    }

    public static RemoteInfo parse(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = REMOTE_PATTERN.matcher(remoteUrl.trim());
        if (matcher.matches()) {
            String host = matcher.group(1);
            String projectPath = matcher.group(2);
            return new RemoteInfo(host, projectPath);
        }
        return null;
    }
}
