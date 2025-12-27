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
@Table(name = "documents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_document_type_year_slug", columnNames = { "type", "year", "slug" })
})
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String link;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String field;

    @Column(columnDefinition = "TEXT")
    private String authors;

    @Column(columnDefinition = "TEXT")
    private String source;

    @Column(columnDefinition = "TEXT")
    private String citation;

    @Column(columnDefinition = "text[]")
    private String[] keywords;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String slug;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
