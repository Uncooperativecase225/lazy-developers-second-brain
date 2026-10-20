package com.lazybrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ProjectDtos {
    private ProjectDtos() {}
    public record CreateRequest(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 1024) String repositoryPath) {}
    public record Response(Long id, String name, String repositoryPath, String defaultBranch, Instant createdAt, Instant lastAccessedAt) {}
}
