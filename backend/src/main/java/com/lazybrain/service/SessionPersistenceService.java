package com.lazybrain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lazybrain.entity.DevelopmentSession;
import com.lazybrain.entity.Project;
import com.lazybrain.entity.ProjectMemory;
import com.lazybrain.entity.Task;
import com.lazybrain.exception.NotFoundException;
import com.lazybrain.repository.DevelopmentSessionRepository;
import com.lazybrain.repository.ProjectMemoryRepository;
import com.lazybrain.repository.ProjectRepository;
import com.lazybrain.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SessionPersistenceService {
    private final ProjectRepository projects;
    private final DevelopmentSessionRepository sessions;
    private final ProjectMemoryRepository memories;
    private final TaskRepository tasks;
    private final ObjectMapper mapper;

    public SessionPersistenceService(ProjectRepository projects, DevelopmentSessionRepository sessions,
                                    ProjectMemoryRepository memories, TaskRepository tasks, ObjectMapper mapper) {
        this.projects = projects; this.sessions = sessions; this.memories = memories; this.tasks = tasks; this.mapper = mapper;
    }

    @Transactional
    public Long persist(Long projectId, AiService.Analysis analysis, String branch, String head, boolean aiGenerated) {
        Project project = projects.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        DevelopmentSession session = sessions.save(new DevelopmentSession(project, analysis.summary(), toJson(analysis.completedTasks()),
                toJson(analysis.pendingTasks()), analysis.nextAction(), branch, head));
        memories.save(new ProjectMemory(project, analysis.summary(), "SESSION_SUMMARY", "session:" + session.getId()));
        if (aiGenerated && !analysis.pendingTasks().isEmpty()) {
            Set<String> existingTitles = new HashSet<>();
            tasks.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                    .filter(t -> t.getStatus() != Task.Status.DONE && t.getStatus() != Task.Status.DISMISSED)
                    .forEach(t -> existingTitles.add(t.getTitle().toLowerCase(java.util.Locale.ROOT)));
            for (String pending : analysis.pendingTasks()) {
                if (existingTitles.add(pending.toLowerCase(java.util.Locale.ROOT))) {
                    tasks.save(new Task(project, pending, "Suggested by AI session analysis; verify before acting.", "AI_SESSION", "session:" + session.getId()));
                }
            }
        }
        return session.getId();
    }

    private String toJson(List<String> values) {
        try { return mapper.writeValueAsString(values); }
        catch (JsonProcessingException ex) { return "[]"; }
    }
}
