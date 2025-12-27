package com.guidelinex.config;

import com.guidelinex.model.SystemStats;
import com.guidelinex.repository.SystemStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DataInitializer ensures that the singleton tracking row exists in the
 * database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SystemStatsRepository systemStatsRepository;

    @Override
    public void run(String... args) {
        if (!systemStatsRepository.existsById(1L)) {
            log.info("Initializing system_stats table with singleton tracking row...");
            SystemStats stats = SystemStats.builder()
                    .id(1L)
                    .visitCount(0L)
                    .searchCount(0L)
                    .build();
            systemStatsRepository.save(stats);
        }
    }
}
