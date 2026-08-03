package com.example.ainews.domain.keywordrank;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "keyword_search_ranks")
public class KeywordSearchRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private Integer searchCount = 1;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected KeywordSearchRank() {
    }

    public KeywordSearchRank(String keyword, LocalDate weekStart) {
        this.keyword = keyword;
        this.weekStart = weekStart;
        this.searchCount = 1;
    }

    public Long getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public Integer getSearchCount() {
        return searchCount;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}