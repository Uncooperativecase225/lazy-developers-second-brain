package com.lazybrain.controller;

import com.lazybrain.dto.ProjectDtos;
import com.lazybrain.dto.SessionDtos;
import com.lazybrain.entity.Project;
import com.lazybrain.service.GitService;
import com.lazybrain.service.ProjectService;
import com.lazybrain.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projects;
    private final GitService git;
    private final SessionService sessions;
    public ProjectController(ProjectService projects, GitService git, SessionService sessions) { this.projects = projects; this.git = git; this.sessions = sessions; }

    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public ProjectDtos.Response create(@Valid @RequestBody ProjectDtos.CreateRequest request) { return projects.create(request); }
    @GetMapping public List<ProjectDtos.Response> list() { return projects.list(); }
    @GetMapping("/{id}") public ProjectDtos.Response get(@PathVariable Long id) {
        Project p = projects.get(id);
        return new ProjectDtos.Response(p.getId(), p.getName(), p.getRepositoryPath(), p.getDefaultBranch(), p.getCreatedAt(), p.getLastAccessedAt());
    }
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { projects.delete(id); }

    @GetMapping("/{id}/activity") public Map<String, String> activity(@PathVariable Long id) {
        Project p = projects.getAndTouch(id); Path path = Path.of(p.getRepositoryPath());
        return Map.of("branch", git.branch(path).orElse("(detached HEAD)"), "headCommit", git.headCommit(path).orElse("(no commits)"),
                "recentCommits", git.recentLog(path), "workingTree", git.status(path), "diffStat", git.diffStat(path), "changedFiles", git.recentFiles(path));
    }
    @PostMapping("/{id}/resume") public SessionDtos.ResumeResponse resume(@PathVariable Long id) { return sessions.resume(id); }
    @GetMapping("/{id}/sessions") public List<SessionDtos.Response> sessions(@PathVariable Long id) { return this.sessions.list(id); }
}
