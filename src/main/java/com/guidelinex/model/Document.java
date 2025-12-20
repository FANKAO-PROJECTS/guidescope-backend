package com.guidelinex.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(columnDefinition = "text[]")
    private String[] keywords;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
