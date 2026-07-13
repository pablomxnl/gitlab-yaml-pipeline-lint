package org.ideplugins.ci_pipeline_lint.settings;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GitHelperTest {

    @Test
    void testGitlabRemoteParser() {
        // SSH format
        GitlabRemoteParser.RemoteInfo info1 = GitlabRemoteParser.parse("git@gitlab.com:pablomxnl/gitlab-yaml-pipeline-lint.git");
        assertNotNull(info1);
        assertEquals("gitlab.com", info1.host);
        assertEquals("pablomxnl/gitlab-yaml-pipeline-lint", info1.projectPath);

        // HTTPS format
        GitlabRemoteParser.RemoteInfo info2 = GitlabRemoteParser.parse("https://gitlab.com/pablomxnl/gitlab-yaml-pipeline-lint.git");
        assertNotNull(info2);
        assertEquals("gitlab.com", info2.host);
        assertEquals("pablomxnl/gitlab-yaml-pipeline-lint", info2.projectPath);

        // Custom Host and Port (SSH)
        GitlabRemoteParser.RemoteInfo info3 = GitlabRemoteParser.parse("ssh://git@gitlab.mycompany.com:2222/group/subgroup/project.git");
        assertNotNull(info3);
        assertEquals("gitlab.mycompany.com", info3.host);
        assertEquals("group/subgroup/project", info3.projectPath);

        // Custom Host (HTTPS)
        GitlabRemoteParser.RemoteInfo info4 = GitlabRemoteParser.parse("https://gitlab.mycompany.com/group/subgroup/project");
        assertNotNull(info4);
        assertEquals("gitlab.mycompany.com", info4.host);
        assertEquals("group/subgroup/project", info4.projectPath);

        // HTTPS with credentials
        GitlabRemoteParser.RemoteInfo info5 = GitlabRemoteParser.parse("https://oauth2:glpat-token@gitlab.com/group/project.git");
        assertNotNull(info5);
        assertEquals("gitlab.com", info5.host);
        assertEquals("group/project", info5.projectPath);
    }

    @Test
    void testGitRemoteReaderNormal(@TempDir Path tempDir) throws IOException {
        Path gitDir = Files.createDirectory(tempDir.resolve(".git"));
        Path configFile = gitDir.resolve("config");
        String configContent = """
                [core]
                	repositoryformatversion = 0
                	filemode = true
                	bare = false
                	logallrefupdates = true
                [remote "origin"]
                	url = git@gitlab.com:pablomxnl/gitlab-yaml-pipeline-lint.git
                	fetch = +refs/heads/*:refs/remotes/origin/*
                [branch "main"]
                	remote = origin
                	merge = refs/heads/main
                """;
        Files.writeString(configFile, configContent);

        Project mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getBasePath()).thenReturn(tempDir.toAbsolutePath().toString());

        String remoteUrl = GitRemoteReader.getRemoteUrl(mockProject);
        assertEquals("git@gitlab.com:pablomxnl/gitlab-yaml-pipeline-lint.git", remoteUrl);
    }

    @Test
    void testGitRemoteReaderWorktree(@TempDir Path tempDir) throws IOException {
        // Prepare main repository config directory
        Path realGitDir = Files.createDirectory(tempDir.resolve("real-git-dir"));
        Path configFile = realGitDir.resolve("config");
        String configContent = """
                [remote "origin"]
                	url = https://gitlab.mycompany.com/group/project.git
                """;
        Files.writeString(configFile, configContent);

        // Prepare worktree directory
        Path worktreeDir = Files.createDirectory(tempDir.resolve("worktree"));
        Path gitFile = worktreeDir.resolve(".git");
        Files.writeString(gitFile, "gitdir: " + realGitDir.toAbsolutePath());

        Project mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getBasePath()).thenReturn(worktreeDir.toAbsolutePath().toString());

        String remoteUrl = GitRemoteReader.getRemoteUrl(mockProject);
        assertEquals("https://gitlab.mycompany.com/group/project.git", remoteUrl);
    }
}

