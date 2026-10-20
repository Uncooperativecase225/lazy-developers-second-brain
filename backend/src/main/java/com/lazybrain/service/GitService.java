package com.lazybrain.service;

import com.lazybrain.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GitService {
    private final long timeoutSeconds;
    private final int maxOutputBytes;
    public GitService(@Value("${app.git.command-timeout-seconds:8}") long timeoutSeconds,
                      @Value("${app.git.max-output-bytes:200000}") int maxOutputBytes) {
        this.timeoutSeconds = timeoutSeconds; this.maxOutputBytes = maxOutputBytes;
    }

    public boolean isRepository(Path path) {
        Result result = run(path, List.of("rev-parse", "--is-inside-work-tree"));
        return result.exitCode() == 0 && result.stdout().trim().equals("true");
    }
    public Optional<Path> repositoryRoot(Path path) {
        Result result = run(path, List.of("rev-parse", "--show-toplevel"));
        if (result.exitCode() != 0 || result.stdout().isBlank()) return Optional.empty();
        try { return Optional.of(Path.of(result.stdout().trim()).toRealPath()); }
        catch (IOException | RuntimeException ex) { return Optional.empty(); }
    }
    public Optional<String> branch(Path path) {
        Result result = run(path, List.of("branch", "--show-current"));
        return result.exitCode() == 0 ? Optional.of(result.stdout().trim()).filter(s -> !s.isBlank()).map(s -> s.substring(0, Math.min(255, s.length()))) : Optional.empty();
    }
    public Optional<String> headCommit(Path path) {
        Result result = run(path, List.of("rev-parse", "--short", "HEAD"));
        return result.exitCode() == 0 ? Optional.of(result.stdout().trim()).filter(s -> !s.isBlank()) : Optional.empty();
    }
    public String recentLog(Path path) {
        Result result = run(path, List.of("log", "-n", "12", "--date=short", "--pretty=format:%h | %ad | %s"));
        if (result.exitCode() != 0) return "No commits available yet.";
        return trim(result.stdout(), 9000);
    }
    public String status(Path path) {
        Result result = run(path, List.of("status", "--short", "--untracked-files=no"));
        if (result.exitCode() != 0) return "Git status could not be read.";
        String out = result.stdout().trim();
        return out.isBlank() ? "Working tree clean (untracked files not included)." : trim(out, 7000);
    }
    public String diffStat(Path path) {
        Result result = run(path, List.of("diff", "--no-ext-diff", "--no-textconv", "--stat", "HEAD"));
        if (result.exitCode() != 0) return "Diff summary unavailable (the repository may not have a HEAD commit).";
        String out = result.stdout().trim();
        return out.isBlank() ? "No tracked working-tree diff." : trim(out, 7000);
    }
    public String trackedDiff(Path path) {
        Result result = run(path, List.of("diff", "--no-ext-diff", "--no-textconv", "--unified=2", "HEAD", "--", "."));
        if (result.exitCode() != 0) return "Diff unavailable.";
        return trim(result.stdout(), 12000);
    }
    public String recentFiles(Path path) {
        Result result = run(path, List.of("diff", "--name-only", "HEAD"));
        if (result.exitCode() != 0) return "Not available";
        return trim(result.stdout().isBlank() ? "No tracked changed files." : result.stdout(), 3000);
    }

    private Result run(Path path, List<String> args) {
        Process process = null;
        CompletableFuture<String> outputFuture = null;
        AtomicBoolean truncated = new AtomicBoolean(false);
        try {
            List<String> command = new java.util.ArrayList<>();
            command.add("git"); command.add("-c"); command.add("core.fsmonitor=false");
            command.add("-c"); command.add("core.pager=cat");
            command.add("-C"); command.add(path.toString()); command.addAll(args);
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            Process running = process;
            outputFuture = CompletableFuture.supplyAsync(() -> {
                try (InputStream input = running.getInputStream(); ByteArrayOutputStream captured = new ByteArrayOutputStream(Math.min(maxOutputBytes, 8192))) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        int remaining = maxOutputBytes - captured.size();
                        if (remaining > 0) captured.write(buffer, 0, Math.min(remaining, count));
                        if (count > remaining) truncated.set(true);
                    }
                    String value = captured.toString(StandardCharsets.UTF_8);
                    return truncated.get() ? value + "\n[output truncated]" : value;
                } catch (IOException ex) {
                    throw new java.util.concurrent.CompletionException(ex);
                }
            });
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
                if (outputFuture != null) outputFuture.cancel(true);
                return new Result(-1, "Git command timed out");
            }
            String output = outputFuture.get(2, TimeUnit.SECONDS);
            return new Result(process.exitValue(), output);
        } catch (IOException ex) {
            throw new BadRequestException("Git could not be executed. Install Git and ensure it is on PATH.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (process != null) process.destroyForcibly();
            if (outputFuture != null) outputFuture.cancel(true);
            return new Result(-1, "Git command interrupted");
        } catch (ExecutionException | TimeoutException ex) {
            if (process != null) process.destroyForcibly();
            if (outputFuture != null) outputFuture.cancel(true);
            return new Result(-1, "Could not safely capture Git output");
        }
    }
    private String trim(String text, int max) { return text.length() <= max ? text : text.substring(0, max) + "\n[context truncated]"; }
    private record Result(int exitCode, String stdout) {}
}
