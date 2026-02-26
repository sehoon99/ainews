package com.example.ainews.domain.keywordstat;

public record KeywordChange(
        String keyword,
        ChangeType changeType,
        int currentRank,
        int previousRank,
        int totalCount,
        int aiCount
) {

    public enum ChangeType {
        NEW_ENTRY,
        RANK_UP,
        RANK_DOWN,
        DROPPED
    }

    public int rankDiff() {
        return previousRank - currentRank;
    }
}