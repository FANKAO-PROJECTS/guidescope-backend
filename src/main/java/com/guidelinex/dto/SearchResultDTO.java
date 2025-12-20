package com.guidelinex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultDTO {
    private UUID id;
    private String type;
    private String title;
    private Integer year;
    private String link;
    private String[] keywords;
    private Double score;
}
