package com.example.ainews.domain.keywordrank.dto;

import com.example.ainews.domain.keywordrank.KeywordSearchRank;

import java.time.LocalDate;

public record KeywordRankResponse(
        Long id,
        String keyword,
        Integer searchCount,
        LocalDate weekStart
) {
    public static KeywordRankResponse from(KeywordSearchRank entity) {
        return new KeywordRankResponse(
                entity.getId(),
                entity.getKeyword(),
                entity.getSearchCount(),
                entity.getWeekStart()
        );
    }
}