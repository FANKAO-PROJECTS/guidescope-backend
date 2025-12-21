package com.guidelinex.controller;

import com.guidelinex.dto.SearchCapabilitiesDTO;
import com.guidelinex.dto.SearchResponseDTO;
import com.guidelinex.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SearchController exposes the primary REST interface for GuidelineX.
 * 
 * Responsibilities:
 * - Expose read-only /search endpoint
 * - Expose read-only /search/capabilities endpoint
 * - Map HTTP query parameters to service layer
 * - Provide a stable, documented API contract via OpenAPI
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Search", description = "Endpoints for clinical document searching")
public class SearchController {

    private final SearchService searchService;

    /**
     * Executes a clinical document search.
     * 
     * @param query    Keywords for matching against Title and Keywords
     * @param types    Optional multi-value filter for document types
     * @param region   Optional regional filter (US, UK, EU, etc.)
     * @param field    Optional specialty filter (Cardiology, Oncology, etc.)
     * @param yearFrom Minimum publication year
     * @param yearTo   Maximum publication year
     * @param pageable Paging parameters (page, size, sort)
     * @return SearchResponseDTO containing results and total count
     */
    @Operation(summary = "Search clinical documents", description = "Performs a full-text search with optional filters for type, region, field, and year range.")
    @ApiResponse(responseCode = "200", description = "Successful search execution")
    @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @GetMapping("/search")
    public SearchResponseDTO search(
            @Parameter(description = "Keywords to search for in title and keywords") @RequestParam(value = "q", required = false) String query,
            @Parameter(description = "Filter by document types (guideline, consensus, etc.)") @RequestParam(value = "type", required = false) String[] types,
            @Parameter(description = "Filter by region (US, UK, EU, etc.)") @RequestParam(value = "region", required = false) String region,
            @Parameter(description = "Filter by specialty field (Cardiology, Oncology, etc.)") @RequestParam(value = "field", required = false) String field,
            @Parameter(description = "Minimum publication year") @RequestParam(value = "year_from", required = false) Integer yearFrom,
            @Parameter(description = "Maximum publication year") @RequestParam(value = "year_to", required = false) Integer yearTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return searchService.search(query, types, region, field, yearFrom, yearTo, pageable);
    }

    /**
     * Exposes dynamic search capabilities derived from the database.
     * Aligned with docs/search-contract.v1.json.
     * 
     * @return SearchCapabilitiesDTO containing available filters and ranges
     */
    @Operation(summary = "Get search capabilities", description = "Returns dynamic clinical dimensions (types, regions, fields, year ranges) for UI filter initialization.")
    @ApiResponse(responseCode = "200", description = "Capabilities retrieved successfully")
    @GetMapping("/search/capabilities")
    public SearchCapabilitiesDTO getCapabilities() {
        return searchService.getCapabilities();
    }

    /**
     * Provides autocomplete suggestions for search assistance.
     * 
     * @param query Partial search term (minimum 3 characters)
     * @return List of up to 5 suggestions
     */
    @Operation(summary = "Get autocomplete suggestions", description = "Returns up to 5 search suggestions based on document titles and keywords. Requires minimum 3 characters.")
    @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully")
    @GetMapping("/search/autocomplete")
    public com.guidelinex.dto.AutocompleteResponseDTO getAutocomplete(
            @Parameter(description = "Partial search term (min 3 chars)") @RequestParam("q") String query) {
        List<String> suggestions = searchService.getAutocompleteSuggestions(query);
        return new com.guidelinex.dto.AutocompleteResponseDTO(suggestions);
    }
}
