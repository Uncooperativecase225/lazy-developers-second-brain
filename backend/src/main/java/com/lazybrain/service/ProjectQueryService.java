package com.lazybrain.service;

import com.lazybrain.dto.AskDtos;
import com.lazybrain.entity.Project;
import com.lazybrain.entity.ProjectMemory;
import com.lazybrain.repository.ProjectMemoryRepository;
import org.springframework.stereotype.Service;
import java.nio.file.Path;

@Service
public class ProjectQueryService {
    private final ProjectService projects;
    private final GitService git;
    private final AiService ai;
    private final SessionService sessions;
    private final ProjectMemoryRepository memories;
    public ProjectQueryService(ProjectService projects, GitService git, AiService ai, SessionService sessions, ProjectMemoryRepository memories) {
        this.projects = projects; this.git = git; this.ai = ai; this.sessions = sessions; this.memories = memories;
    }
    public AskDtos.Response ask(Long projectId, String question) {
        Project p = projects.get(projectId); Path path = Path.of(p.getRepositoryPath());
        String branch = git.branch(path).orElse("(detached HEAD)");
        String answer = ai.answer(question, p.getName(), branch, git.recentLog(path), git.status(path), git.trackedDiff(path),
                sessions.sessionContext(projectId), memories.findTop20ByProjectIdOrderByCreatedAtDesc(projectId).stream()
                        .map(ProjectMemory::getContent).reduce((a,b) -> a + "\n" + b).orElse("No saved project memories."));
        return new AskDtos.Response(answer, true);
    }
}
