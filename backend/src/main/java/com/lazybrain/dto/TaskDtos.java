package com.lazybrain.dto;

import com.lazybrain.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class TaskDtos {
    private TaskDtos() {}
    public record CreateRequest(@NotBlank @Size(max = 240) String title, @Size(max = 2000) String description) {}
    public record UpdateRequest(@Size(max = 240) String title, @Size(max = 2000) String description, Task.Status status) {}
    public record Response(Long id, Long projectId, String title, String description, Task.Status status, String source,
                           String sourceReference, Instant createdAt, Instant updatedAt) {}
}
