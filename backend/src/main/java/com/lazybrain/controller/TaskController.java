package com.lazybrain.controller;

import com.lazybrain.dto.TaskDtos;
import com.lazybrain.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {
    private final TaskService tasks;
    public TaskController(TaskService tasks) { this.tasks = tasks; }
    @GetMapping public List<TaskDtos.Response> list(@PathVariable Long projectId) { return tasks.list(projectId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public TaskDtos.Response create(@PathVariable Long projectId, @Valid @RequestBody TaskDtos.CreateRequest request) { return tasks.create(projectId, request); }
    @PatchMapping("/{taskId}") public TaskDtos.Response update(@PathVariable Long projectId, @PathVariable Long taskId, @Valid @RequestBody TaskDtos.UpdateRequest request) { return tasks.update(projectId, taskId, request); }
    @DeleteMapping("/{taskId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long projectId, @PathVariable Long taskId) { tasks.delete(projectId, taskId); }
}
