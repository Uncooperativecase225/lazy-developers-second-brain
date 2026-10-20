package com.lazybrain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_memories", indexes = @Index(name = "idx_memories_project_created", columnList = "project_id,created_at"))
public class ProjectMemory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @Column(nullable = false, length = 4000) private String content;
    @Column(name = "source_type", nullable = false, length = 40) private String sourceType;
    @Column(name = "source_reference", length = 1024) private String sourceReference;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected ProjectMemory() {}
    public ProjectMemory(Project project, String content, String sourceType, String sourceReference) {
        this.project = project; this.content = content; this.sourceType = sourceType; this.sourceReference = sourceReference; this.createdAt = Instant.now();
    }
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getContent() { return content; }
    public String getSourceType() { return sourceType; }
    public String getSourceReference() { return sourceReference; }
    public Instant getCreatedAt() { return createdAt; }
}
