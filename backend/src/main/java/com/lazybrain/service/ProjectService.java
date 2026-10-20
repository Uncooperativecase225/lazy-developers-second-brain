package com.lazybrain.service;

import com.lazybrain.dto.ProjectDtos;
import com.lazybrain.entity.Project;
import com.lazybrain.exception.BadRequestException;
import com.lazybrain.exception.NotFoundException;
import com.lazybrain.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final GitService git;
    public ProjectService(ProjectRepository projects, GitService git) { this.projects = projects; this.git = git; }

    @Transactional
    public ProjectDtos.Response create(ProjectDtos.CreateRequest request) {
        Path canonical;
        try {
            Path submitted = Path.of(request.repositoryPath().trim());
            if (!submitted.isAbsolute()) throw new BadRequestException("Repository path must be absolute");
            canonical = submitted.toRealPath();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new BadRequestException("Repository path does not exist or cannot be accessed");
        }
        if (!Files.isDirectory(canonical)) throw new BadRequestException("Repository path must be a directory");
        if (!git.isRepository(canonical)) throw new BadRequestException("The selected directory is not a Git repository");
        Path repositoryRoot = git.repositoryRoot(canonical).orElseThrow(() -> new BadRequestException("Could not resolve the Git repository root"));
        if (!repositoryRoot.equals(canonical)) throw new BadRequestException("Select the repository root directory, not a subdirectory");
        String path = canonical.toString();
        if (path.length() > 700) throw new BadRequestException("Repository path is longer than the supported 700 characters");
        if (projects.findByRepositoryPath(path).isPresent()) throw new BadRequestException("This repository is already registered");
        String branch = git.branch(canonical).orElse(null);
        Project project = projects.save(new Project(request.name().trim(), path, branch));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.Response> list() { return projects.findAll().stream().map(this::toResponse).toList(); }

    @Transactional
    public Project getAndTouch(Long id) {
        Project project = projects.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
        project.touch();
        return project;
    }

    @Transactional(readOnly = true)
    public Project get(Long id) { return projects.findById(id).orElseThrow(() -> new NotFoundException("Project not found")); }

    @Transactional
    public void delete(Long id) {
        Project project = projects.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
        projects.delete(project);
    }

    private ProjectDtos.Response toResponse(Project p) {
        return new ProjectDtos.Response(p.getId(), p.getName(), p.getRepositoryPath(), p.getDefaultBranch(), p.getCreatedAt(), p.getLastAccessedAt());
    }
}
