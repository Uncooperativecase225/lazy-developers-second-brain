package com.lazybrain.dto;

import java.time.Instant;
import java.util.List;

public final class SessionDtos {
    private SessionDtos() {}
    public record Response(Long id, Long projectId, String summary, List<String> completedTasks, List<String> pendingTasks,
                           String nextAction, String branchName, String headCommit, Instant createdAt) {}
    public record ResumeResponse(String projectName, String branchName, String headCommit, String workingTreeSummary,
                                 String summary, List<String> completedTasks, List<String> pendingTasks,
                                 String nextAction, Long sessionId, boolean aiGenerated) {}
}
