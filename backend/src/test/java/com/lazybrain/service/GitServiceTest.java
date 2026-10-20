package com.lazybrain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class GitServiceTest {
    @TempDir Path temp;

    @Test void recognizesGitRepositoryAndReadsStatus() throws Exception {
        Process init = new ProcessBuilder("git", "init", temp.toString()).redirectErrorStream(true).start();
        assertTrue(init.waitFor(5, TimeUnit.SECONDS));
        assertEquals(0, init.exitValue());
        GitService service = new GitService(3, 20000);
        assertTrue(service.isRepository(temp));
        assertTrue(service.branch(temp).isEmpty() || service.branch(temp).orElseThrow().length() < 100);
        Files.writeString(temp.resolve("README.md"), "# test\n");
        Process add = new ProcessBuilder("git", "-C", temp.toString(), "add", "README.md").redirectErrorStream(true).start();
        assertTrue(add.waitFor(5, TimeUnit.SECONDS));
        assertEquals(0, add.exitValue());
        assertTrue(service.status(temp).contains("README.md"));
    }

    @Test void rejectsNonRepository() throws Exception {
        Path plain = Files.createDirectory(temp.resolve("plain"));
        GitService service = new GitService(3, 20000);
        assertFalse(service.isRepository(plain));
    }
}
