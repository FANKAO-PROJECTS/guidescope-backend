package com.guidelinex.service;

import com.guidelinex.model.SystemStats;
import com.guidelinex.repository.SystemStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SystemStatsService centralizes all platform analytics tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemStatsService {

    private final SystemStatsRepository systemStatsRepository;

    @Transactional(readOnly = true)
    public SystemStats getStats() {
        return systemStatsRepository.findById(1L)
                .orElseGet(() -> SystemStats.builder().id(1L).visitCount(0L).searchCount(0L).build());
    }

    public void recordVisit() {
        try {
            systemStatsRepository.incrementVisitCount();
            log.debug("Visit recorded");
        } catch (Exception e) {
            log.error("Failed to record visit: {}", e.getMessage());
        }
    }

    public void recordSearch() {
        try {
            systemStatsRepository.incrementSearchCount();
            log.debug("Search recorded");
        } catch (Exception e) {
            log.error("Failed to record search: {}", e.getMessage());
        }
    }
}
