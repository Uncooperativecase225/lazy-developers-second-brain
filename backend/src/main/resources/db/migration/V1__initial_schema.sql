CREATE TABLE projects (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  repository_path VARCHAR(700) NOT NULL,
  default_branch VARCHAR(255) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  last_accessed_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_projects_repository_path UNIQUE (repository_path)
) ENGINE=InnoDB;

CREATE TABLE development_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  summary VARCHAR(4000) NOT NULL,
  completed_tasks VARCHAR(4000) NULL,
  pending_tasks VARCHAR(4000) NULL,
  next_action VARCHAR(1000) NULL,
  branch_name VARCHAR(255) NULL,
  head_commit VARCHAR(64) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_sessions_project_created (project_id, created_at),
  CONSTRAINT fk_sessions_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE tasks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  title VARCHAR(240) NOT NULL,
  description VARCHAR(2000) NULL,
  status VARCHAR(20) NOT NULL,
  source VARCHAR(30) NOT NULL,
  source_reference VARCHAR(1024) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_tasks_project_status (project_id, status),
  KEY idx_tasks_project_created (project_id, created_at),
  CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE project_memories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  content VARCHAR(4000) NOT NULL,
  source_type VARCHAR(40) NOT NULL,
  source_reference VARCHAR(1024) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_memories_project_created (project_id, created_at),
  CONSTRAINT fk_memories_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB;
