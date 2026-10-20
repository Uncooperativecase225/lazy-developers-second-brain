package com.lazybrain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tasks", indexes = { @Index(name = "idx_tasks_project_status", columnList = "project_id,status"), @Index(name = "idx_tasks_project_created", columnList = "project_id,created_at") })
public class Task {
    public enum Status { OPEN, IN_PROGRESS, DONE, DISMISSED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @Column(nullable = false, length = 240) private String title;
    @Column(length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.OPEN;
    @Column(nullable = false, length = 30) private String source = "MANUAL";
    @Column(name = "source_reference", length = 1024) private String sourceReference;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Task() {}
    public Task(Project project, String title, String description, String source, String sourceReference) {
        this.project = project; this.title = title; this.description = description;
        this.source = source == null ? "MANUAL" : source; this.sourceReference = sourceReference;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); if (updatedAt == null) updatedAt = Instant.now(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    public void update(String title, String description, Status status) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (status != null) this.status = status;
        this.updatedAt = Instant.now();
    }
    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public String getSource() { return source; }
    public String getSourceReference() { return sourceReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
