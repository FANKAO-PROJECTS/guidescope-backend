package com.guidelinex.service;

import com.guidelinex.dto.SearchResultDTO;
import com.guidelinex.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final DocumentRepository documentRepository;

    public List<SearchResultDTO> search(String query, String[] types, Integer yearFrom, Integer yearTo, int limit,
            int offset) {

        log.info("Performing search with query: '{}', types: {}, yearFrom: {}, yearTo: {}, limit: {}, offset: {}",
                query, types, yearFrom, yearTo, limit, offset);

        boolean hasFilters = (types != null && types.length > 0) || yearFrom != null || yearTo != null;
        boolean hasQuery = query != null && !query.trim().isEmpty();

        if (!hasQuery && !hasFilters) {
            log.debug("Aborting search: no query and no filters provided");
            return new ArrayList<>();
        }

        // Validate range-based constraints
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("Limit must be between 1 and 50");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }

        List<Object[]> results = documentRepository.searchDocuments(
                query.trim(),
                types,
                yearFrom,
                yearTo,
                limit,
                offset);

        log.info("Found {} results for query: '{}'", results.size(), query);

        return results.stream().map(row -> {
            try {
                // Safely handle keywords (java.sql.Array to String[])
                String[] keywords = null;
                if (row[5] != null) {
                    if (row[5] instanceof java.sql.Array javaArray) {
                        keywords = (String[]) javaArray.getArray();
                    } else if (row[5] instanceof String[] strArray) {
                        keywords = strArray;
                    }
                }

                // Safely handle score (Float from ts_rank to Double)
                Double score = 0.0;
                if (row[8] != null) {
                    if (row[8] instanceof Float f) {
                        score = f.doubleValue();
                    } else if (row[8] instanceof Double d) {
                        score = d;
                    } else if (row[8] instanceof Number n) {
                        score = n.doubleValue();
                    }
                }

                return SearchResultDTO.builder()
                        .id((UUID) row[0])
                        .type((String) row[1])
                        .title((String) row[2])
                        .year((Integer) row[3])
                        .link((String) row[4])
                        .keywords(keywords)
                        .score(score)
                        .build();
            } catch (Exception e) {
                log.error("Error mapping search result row: {}", e.getMessage(), e);
                throw new RuntimeException("Error mapping search result", e);
            }
        }).toList();
    }
}
