package com.lazybrain.repository;

import com.lazybrain.entity.DevelopmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DevelopmentSessionRepository extends JpaRepository<DevelopmentSession, Long> {
    List<DevelopmentSession> findTop30ByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<DevelopmentSession> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);
}
