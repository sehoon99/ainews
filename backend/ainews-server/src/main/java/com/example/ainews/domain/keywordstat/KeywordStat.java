package com.example.ainews.domain.keywordstat;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "keyword_stats")
public class KeywordStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;

    @Column(name = "ai_count", nullable = false)
    private Integer aiCount = 0;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected KeywordStat() {
    }

    public KeywordStat(String keyword, int totalCount, int aiCount, int rankPosition, LocalDate statDate) {
        this.keyword = keyword;
        this.totalCount = totalCount;
        this.aiCount = aiCount;
        this.rankPosition = rankPosition;
        this.statDate = statDate;
    }

    public Long getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public Integer getAiCount() {
        return aiCount;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}