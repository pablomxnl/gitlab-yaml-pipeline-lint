package org.ideplugins.ci_pipeline_lint.settings;

import com.intellij.openapi.project.Project;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

/**
 * Utility to obtain a git remote url for a project.
 * Prefer using Git4Idea APIs (more accurate in multi-root/worktree cases).
 * Fall back to reading the repository config on disk to support unit tests and environments
 * where the Git plugin may not be available.
 */
public class GitRemoteReader {
    public static String getRemoteUrl(Project project) {
        if (project != null) {
            try {
                // Use Git4Idea API directly (now a hard dependency)
                GitRepositoryManager repoManager = GitRepositoryManager.getInstance(project);
                Collection<GitRepository> repos = repoManager.getRepositories();
                if (!repos.isEmpty()) {
                    // prefer repository with remotes and origin first
                    for (GitRepository repo : repos) {
                        for (GitRemote remote : repo.getRemotes()) {
                            if ("origin".equals(remote.getName())) {
                                String url = remote.getFirstUrl();
                                if (url != null && !url.isEmpty()) {
                                    return url;
                                }
                            }
                        }
                    }
                    // fallback to any remote
                    for (GitRepository repo : repos) {
                        for (GitRemote remote : repo.getRemotes()) {
                            String url = remote.getFirstUrl();
                            if (url != null && !url.isEmpty()) {
                                return url;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // if Git plugin not available or something goes wrong, fall back to file parsing
            }
        }

        // Fallback: parse .git/config on disk (kept for unit tests and non-IDE environments)
        if (project == null || project.getBasePath() == null) {
            return null;
        }
        File projectDir = new File(project.getBasePath());
        File gitFileOrDir = new File(projectDir, ".git");
        if (!gitFileOrDir.exists()) {
            return null;
        }

        File gitDir = gitFileOrDir;
        if (gitFileOrDir.isFile()) {
            // It could be a git worktree file containing "gitdir: /path/to/gitdir"
            try {
                List<String> lines = Files.readAllLines(gitFileOrDir.toPath());
                for (String line : lines) {
                    if (line.startsWith("gitdir:")) {
                        String path = line.substring("gitdir:".length()).trim();
                        File resolved = new File(path);
                        if (!resolved.isAbsolute()) {
                            resolved = new File(projectDir, path);
                        }
                        gitDir = resolved;
                        break;
                    }
                }
            } catch (IOException e) {
                // Ignore and try default
            }
        }

        File configFile = new File(gitDir, "config");
        if (!configFile.exists() || !configFile.isFile()) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(configFile.toPath());
            boolean inRemoteOrigin = false;
            boolean inAnyRemote = false;
            String firstRemoteUrl = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    inRemoteOrigin = trimmed.matches("\\[remote\\s+\"origin\"\\s*]");
                    inAnyRemote = trimmed.matches("\\[remote\\s+\"[^\"]+\"\\s*]");
                } else if (trimmed.startsWith("url")) {
                    int eqIndex = trimmed.indexOf('=');
                    if (eqIndex != -1) {
                        String urlValue = trimmed.substring(eqIndex + 1).trim();
                        if (inRemoteOrigin) {
                            return urlValue;
                        } else if (inAnyRemote && firstRemoteUrl == null) {
                            firstRemoteUrl = urlValue;
                        }
                    }
                }
            }
            if (firstRemoteUrl != null) {
                return firstRemoteUrl;
            }
        } catch (IOException e) {
            // Ignore
        }
        return null;
    }
}




