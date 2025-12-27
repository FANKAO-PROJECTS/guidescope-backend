package com.guidelinex.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SystemStats persists global counters for the platform.
 * It uses a single-row design (id=1) for simplicity.
 */
@Entity
@Table(name = "system_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStats {

    @Id
    private Long id;

    @Column(name = "visit_count", nullable = false)
    @Builder.Default
    private Long visitCount = 0L;

    @Column(name = "search_count", nullable = false)
    @Builder.Default
    private Long searchCount = 0L;
}
