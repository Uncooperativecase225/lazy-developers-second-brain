package com.lazybrain.service;

import com.lazybrain.dto.TaskDtos;
import com.lazybrain.entity.Project;
import com.lazybrain.entity.Task;
import com.lazybrain.exception.NotFoundException;
import com.lazybrain.exception.BadRequestException;
import com.lazybrain.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectService projects;
    public TaskService(TaskRepository tasks, ProjectService projects) { this.tasks = tasks; this.projects = projects; }

    @Transactional(readOnly = true)
    public List<TaskDtos.Response> list(Long projectId) {
        projects.get(projectId);
        return tasks.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(this::toResponse).toList();
    }
    @Transactional
    public TaskDtos.Response create(Long projectId, TaskDtos.CreateRequest request) {
        Project project = projects.get(projectId);
        return toResponse(tasks.save(new Task(project, request.title().trim(), clean(request.description()), "MANUAL", null)));
    }
    @Transactional
    public TaskDtos.Response update(Long projectId, Long taskId, TaskDtos.UpdateRequest request) {
        if (request.title() != null && request.title().isBlank()) throw new BadRequestException("Task title cannot be blank");
        Task task = tasks.findByIdAndProjectId(taskId, projectId).orElseThrow(() -> new NotFoundException("Task not found for this project"));
        task.update(request.title() == null ? null : request.title().trim(), request.description() == null ? null : clean(request.description()), request.status());
        return toResponse(task);
    }
    @Transactional
    public void delete(Long projectId, Long taskId) {
        Task task = tasks.findByIdAndProjectId(taskId, projectId).orElseThrow(() -> new NotFoundException("Task not found for this project"));
        tasks.delete(task);
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private TaskDtos.Response toResponse(Task t) {
        return new TaskDtos.Response(t.getId(), t.getProject().getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getSource(), t.getSourceReference(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
