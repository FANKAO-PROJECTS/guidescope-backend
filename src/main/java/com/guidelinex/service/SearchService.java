package com.guidelinex.service;

import com.guidelinex.dto.SearchCapabilitiesDTO;
import com.guidelinex.dto.SearchResponseDTO;
import com.guidelinex.dto.SearchResultDTO;
import com.guidelinex.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

/**
 * SearchService coordinates the search execution and applies business rules.
 * 
 * Responsibilities:
 * - Normalize and sanitize search input
 * - Validate parameters (e.g. q.length, limit bounds)
 * - Coordinate repository calls for FTS execution
 * - Encapsulate search business rules
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final DocumentRepository documentRepository;

    private final AtomicReference<SearchCapabilitiesDTO> capabilitiesCache = new AtomicReference<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL = TimeUnit.HOURS.toMillis(24);

    /**
     * Exposes dynamic search capabilities derived from the database.
     * Aligned with docs/search-contract.v1.json.
     */
    @Transactional(readOnly = true)
    public SearchCapabilitiesDTO getCapabilities() {
        long now = System.currentTimeMillis();
        SearchCapabilitiesDTO cached = capabilitiesCache.get();

        if (cached != null && (now - lastCacheUpdate) < CACHE_TTL) {
            return cached;
        }

        log.info("Refreshing search capabilities cache from database...");

        List<String> types = documentRepository.findDistinctTypes();
        List<String> regions = documentRepository.findDistinctRegions();
        List<String> fields = documentRepository.findDistinctFields();
        List<Object[]> rangeList = documentRepository.findYearRange();
        SearchCapabilitiesDTO.YearRange yearRange = null;
        if (rangeList != null && !rangeList.isEmpty()) {
            Object[] row = rangeList.get(0);
            if (row != null && row.length >= 2) {
                Integer min = (row[0] != null) ? ((Number) row[0]).intValue() : null;
                Integer max = (row[1] != null) ? ((Number) row[1]).intValue() : null;
                if (min != null || max != null) {
                    yearRange = SearchCapabilitiesDTO.YearRange.builder()
                            .min(min)
                            .max(max)
                            .build();
                }
            }
        }

        SearchCapabilitiesDTO capabilities = SearchCapabilitiesDTO.builder()
                .types(types)
                .regions(regions)
                .fields(fields)
                .yearRange(yearRange)
                .build();

        capabilitiesCache.set(capabilities);
        lastCacheUpdate = now;

        return capabilities;
    }

    /**
     * Performs a normalized search operation.
     * Validates that at least one search criterion is provided.
     */
    @Transactional(readOnly = true)
    public SearchResponseDTO search(String query, String[] types, String region, String field,
            Integer yearFrom, Integer yearTo, String slug,
            Pageable pageable) {

        // Normalize and sanitize search input
        String normalizedQuery = (query == null) ? "" : query.trim().toLowerCase();
        // Replace non-alphanumeric characters (except whitespace) with space to prevent
        // tsquery syntax errors
        normalizedQuery = normalizedQuery.replaceAll("[^a-z0-9\\s]", " ");
        // Collapse multiple spaces
        normalizedQuery = normalizedQuery.replaceAll("\\s+", " ").trim();

        // Format for tsquery: replace spaces with ":* & " and append ":*"
        // This supports the "Related/Partial" search via to_tsquery
        String prefixQuery = "";
        if (!normalizedQuery.isEmpty()) {
            prefixQuery = normalizedQuery.replaceAll("\\s+", ":* & ") + ":*";
        }

        // CRITICAL FIX: For the main query parameter passed to the repository,
        // we must use the original (trimmed) query, NOT the stripped one.
        // This is because the repository uses this parameter for:
        // 1. Exact Title Match (lower(title) = lower(:query)) -> Needs punctuation!
        // 2. websearch_to_tsquery(:query) -> Handles punctuation safely itself!
        //
        // The previous 'normalizedQuery' stripped all special chars, breaking exact
        // matching
        // for titles like "AHA/ACC..." and potentially confusing websearch_to_tsquery.
        String tsQuery = (query == null) ? "" : query.trim();

        log.info(
                "Performing search - Q: '{}', prefixQuery: '{}', Slug: '{}', Types: {}, Region: {}, Field: {}, Year: {}-{}, Pageable: {}",
                normalizedQuery, prefixQuery, slug, types, region, field, yearFrom, yearTo, pageable);

        boolean hasFilters = (types != null && types.length > 0) || region != null || field != null || yearFrom != null
                || yearTo != null;
        boolean hasQuery = !normalizedQuery.isEmpty();
        boolean hasSlug = (slug != null && !slug.isEmpty());

        if (!hasQuery && !hasFilters && !hasSlug) {
            log.debug("Aborting search: no query, no filters, and no slug provided");
            return SearchResponseDTO.builder()
                    .results(new ArrayList<>())
                    .total(0)
                    .limit(pageable.getPageSize())
                    .offset((int) pageable.getOffset())
                    .build();
        }

        Page<Object[]> resultsPage = documentRepository.searchDocuments(
                tsQuery,
                prefixQuery,
                slug,
                types,
                region,
                field,
                yearFrom,
                yearTo,
                pageable);

        long totalCount = resultsPage.getTotalElements();

        log.info("Found {} total results ({} in current page) for query: '{}', slug: '{}'", totalCount,
                resultsPage.getContent().size(), normalizedQuery, slug);

        List<SearchResultDTO> dtos = resultsPage.getContent().stream().map(row -> {
            try {
                // row mapping:
                // 0:id, 1:type, 2:region, 3:field, 4:title, 5:year, 6:link, 7:authors,
                // 8:source, 9:citation, 10:keywords, 11:score

                // Safely handle keywords (java.sql.Array to String[])
                String[] keywords = null;
                if (row[10] != null) {
                    if (row[10] instanceof java.sql.Array javaArray) {
                        keywords = (String[]) javaArray.getArray();
                    } else if (row[10] instanceof String[] strArray) {
                        keywords = strArray;
                    }
                }

                return SearchResultDTO.builder()
                        .id((UUID) row[0])
                        .type((String) row[1])
                        .region((String) row[2])
                        .field((String) row[3])
                        .title((String) row[4])
                        .year((Integer) row[5])
                        .link((String) row[6])
                        .authors((String) row[7])
                        .source((String) row[8])
                        .citation((String) row[9])
                        .keywords(keywords)
                        .build();
            } catch (Exception e) {
                log.error("Error mapping search result row: {}", e.getMessage(), e);
                throw new RuntimeException("Error mapping search result", e);
            }
        }).toList();

        return SearchResponseDTO.builder()
                .results(dtos)
                .total(totalCount)
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .build();
    }

    /**
     * Provides autocomplete suggestions for search assistance.
     * Triggers only for queries with length >= 3.
     */
    @Transactional(readOnly = true)
    public List<com.guidelinex.dto.AutocompleteResponseDTO.Suggestion> getAutocompleteSuggestions(String query) {
        if (query == null || query.trim().length() < 3) {
            return List.of();
        }

        String sanitized = query.trim().toLowerCase();
        // Replace non-alphanumeric characters (except whitespace) with space
        sanitized = sanitized.replaceAll("[^a-z0-9\\s]", " ");
        // Collapse multiple spaces
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        log.info("Fetching autocomplete suggestions for: {}", sanitized);

        try {
            List<Object[]> rows = documentRepository.findAutocompleteSuggestions(sanitized);

            return rows.stream()
                    .filter(row -> row != null && row.length >= 2) // Ensure we have both title and slug
                    .map(row -> {
                        try {
                            String title = (row[0] != null) ? (String) row[0] : "";
                            String slug = (row[1] != null) ? (String) row[1] : "";
                            return new com.guidelinex.dto.AutocompleteResponseDTO.Suggestion(title, slug);
                        } catch (Exception e) {
                            log.error("Error mapping autocomplete suggestion row: {}", e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(suggestion -> suggestion != null && !suggestion.getTitle().isEmpty())
                    .toList();
        } catch (Exception e) {
            log.error("Error fetching autocomplete suggestions for query '{}': {}", sanitized, e.getMessage(), e);
            // Return empty list instead of throwing to prevent 500 errors
            return List.of();
        }
    }
}
