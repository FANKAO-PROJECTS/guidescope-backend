package com.guidelinex.controller;

import com.guidelinex.model.SystemStats;
import com.guidelinex.service.SystemStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SystemStatsController exposes endpoints for platform analytics.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for platform metrics and tracking")
public class SystemStatsController {

    private final SystemStatsService statsService;

    @Operation(summary = "Get platform statistics", description = "Returns global visit and search counts.")
    @GetMapping
    public SystemStats getStats() {
        return statsService.getStats();
    }

    @Operation(summary = "Record a visit", description = "Increments the global visit counter.")
    @PostMapping("/visit")
    public void recordVisit() {
        statsService.recordVisit();
    }
}
