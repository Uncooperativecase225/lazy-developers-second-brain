package com.lazybrain.repository;

import com.lazybrain.entity.ProjectMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectMemoryRepository extends JpaRepository<ProjectMemory, Long> {
    List<ProjectMemory> findTop20ByProjectIdOrderByCreatedAtDesc(Long projectId);
}
