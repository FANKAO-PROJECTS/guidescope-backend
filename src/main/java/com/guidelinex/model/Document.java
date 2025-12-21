package com.guidelinex.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document represents the authoritative clinical record in the 'documents'
 * table.
 * 
 * Characteristics:
 * - Append-only (immutable via API)
 * - Fully synchronized with GuidelineX schema
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String link;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String field;

    @Column(columnDefinition = "text[]")
    private String[] keywords;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
