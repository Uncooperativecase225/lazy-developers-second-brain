package com.lazybrain.repository;

import com.lazybrain.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<Task> findByIdAndProjectId(Long id, Long projectId);
}
