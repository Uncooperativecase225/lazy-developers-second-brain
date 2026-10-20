package com.lazybrain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lazybrain.dto.SessionDtos;
import com.lazybrain.entity.DevelopmentSession;
import com.lazybrain.entity.Project;
import com.lazybrain.repository.DevelopmentSessionRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Path;
import java.util.List;

@Service
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private final ProjectService projectService;
    private final GitService git;
    private final AiService ai;
    private final DevelopmentSessionRepository sessions;
    private final SessionPersistenceService persistence;
    private final ObjectMapper mapper;

    public SessionService(ProjectService projectService, GitService git, AiService ai, DevelopmentSessionRepository sessions, SessionPersistenceService persistence, ObjectMapper mapper) {
        this.projectService = projectService; this.git = git; this.ai = ai; this.sessions = sessions; this.persistence = persistence; this.mapper = mapper;
    }

    public SessionDtos.ResumeResponse resume(Long projectId) {
        Project project = projectService.getAndTouch(projectId);
        Path path = Path.of(project.getRepositoryPath());
        String branch = git.branch(path).orElse("(detached HEAD)");
        String head = git.headCommit(path).orElse("(no commits)");
        String status = git.status(path);
        String recentLog = git.recentLog(path);
        String stat = git.diffStat(path);
        String diff = git.trackedDiff(path);
        String previous = sessions.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .map(s -> "Summary: " + s.getSummary() + "\nNext action: " + s.getNextAction() + "\nPending: " + s.getPendingTasks())
                .orElse("No saved session yet.");

        AiService.Analysis analysis;
        boolean generated = true;
        try {
            analysis = ai.analyze(project.getName(), branch, head, status, recentLog, stat, diff, previous);
        } catch (Exception ex) {
            log.warn("Could not generate AI resume summary for project {}. Saving fallback session.", projectId, ex);
            generated = false;
            analysis = new AiService.Analysis("AI summary unavailable. Review the Git activity and working-tree status below.",
                    List.of(), List.of(), "Review the latest commits and changed files, then save a session summary.");
        }
        Long sessionId = persistence.persist(projectId, analysis, branch, head, generated);
        return new SessionDtos.ResumeResponse(project.getName(), branch, head, status, analysis.summary(), analysis.completedTasks(),
                analysis.pendingTasks(), analysis.nextAction(), sessionId, generated);
    }

    @Transactional(readOnly = true)
    public List<SessionDtos.Response> list(Long projectId) {
        projectService.get(projectId);
        return sessions.findTop30ByProjectIdOrderByCreatedAtDesc(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public String sessionContext(Long projectId) {
        projectService.get(projectId);
        return sessions.findTop30ByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(s -> "[" + s.getCreatedAt() + "] " + s.getSummary() + " Next: " + s.getNextAction())
                .reduce((a, b) -> a + "\n" + b).orElse("No saved sessions.");
    }
    private SessionDtos.Response toResponse(DevelopmentSession s) {
        return new SessionDtos.Response(s.getId(), s.getProject().getId(), s.getSummary(), fromJson(s.getCompletedTasks()),
                fromJson(s.getPendingTasks()), s.getNextAction(), s.getBranchName(), s.getHeadCommit(), s.getCreatedAt());
    }

    private List<String> fromJson(String json) {
        try { return mapper.readValue(json == null ? "[]" : json, new TypeReference<>() {}); }
        catch (Exception ex) { return List.of(); }
    }
}
