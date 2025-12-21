package com.guidelinex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCapabilitiesDTO {
    private List<String> types;
    private List<String> regions;
    private List<String> fields;
    private YearRange yearRange;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearRange {
        private Integer min;
        private Integer max;
    }
}
