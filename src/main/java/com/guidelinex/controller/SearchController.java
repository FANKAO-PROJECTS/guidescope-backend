package com.guidelinex.controller;

import com.guidelinex.dto.SearchResultDTO;
import com.guidelinex.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public List<SearchResultDTO> search(
            @RequestParam("q") String query,
            @RequestParam(value = "type", required = false) String[] types,
            @RequestParam(value = "year_from", required = false) Integer yearFrom,
            @RequestParam(value = "year_to", required = false) Integer yearTo,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        // Enforce max limit
        int cappedLimit = Math.min(limit, 50);
        return searchService.search(query, types, yearFrom, yearTo, cappedLimit, offset);
    }
}
