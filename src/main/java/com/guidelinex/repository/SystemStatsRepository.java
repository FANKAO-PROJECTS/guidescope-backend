package com.guidelinex.repository;

import com.guidelinex.model.SystemStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * SystemStatsRepository handles atomic counter updates for platform analytics.
 */
@Repository
public interface SystemStatsRepository extends JpaRepository<SystemStats, Long> {

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE SystemStats s SET s.visitCount = s.visitCount + 1 WHERE s.id = 1")
    void incrementVisitCount();

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE SystemStats s SET s.searchCount = s.searchCount + 1 WHERE s.id = 1")
    void incrementSearchCount();
}
