package com.lazybrain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projects", uniqueConstraints = @UniqueConstraint(name = "uk_projects_repository_path", columnNames = "repository_path"))
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "repository_path", nullable = false, length = 700) private String repositoryPath;
    @Column(name = "default_branch", length = 255) private String defaultBranch;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "last_accessed_at", nullable = false) private Instant lastAccessedAt;

    protected Project() {}
    public Project(String name, String repositoryPath, String defaultBranch) {
        this.name = name; this.repositoryPath = repositoryPath; this.defaultBranch = defaultBranch;
        this.createdAt = Instant.now(); this.lastAccessedAt = this.createdAt;
    }
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); if (lastAccessedAt == null) lastAccessedAt = Instant.now(); }
    public void touch() { this.lastAccessedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRepositoryPath() { return repositoryPath; }
    public String getDefaultBranch() { return defaultBranch; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAccessedAt() { return lastAccessedAt; }
}
