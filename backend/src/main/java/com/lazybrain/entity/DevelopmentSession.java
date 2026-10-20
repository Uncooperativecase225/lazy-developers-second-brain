package com.lazybrain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "development_sessions", indexes = @Index(name = "idx_sessions_project_created", columnList = "project_id,created_at"))
public class DevelopmentSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @Column(nullable = false, length = 4000) private String summary;
    @Column(name = "completed_tasks", length = 4000) private String completedTasks = "[]";
    @Column(name = "pending_tasks", length = 4000) private String pendingTasks = "[]";
    @Column(name = "next_action", length = 1000) private String nextAction;
    @Column(name = "branch_name", length = 255) private String branchName;
    @Column(name = "head_commit", length = 64) private String headCommit;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected DevelopmentSession() {}
    public DevelopmentSession(Project project, String summary, String completedTasks, String pendingTasks, String nextAction, String branchName, String headCommit) {
        this.project = project; this.summary = summary; this.completedTasks = completedTasks; this.pendingTasks = pendingTasks;
        this.nextAction = nextAction; this.branchName = branchName; this.headCommit = headCommit; this.createdAt = Instant.now();
    }
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getSummary() { return summary; }
    public String getCompletedTasks() { return completedTasks; }
    public String getPendingTasks() { return pendingTasks; }
    public String getNextAction() { return nextAction; }
    public String getBranchName() { return branchName; }
    public String getHeadCommit() { return headCommit; }
    public Instant getCreatedAt() { return createdAt; }
}
