package com.example.ainews.domain.keywordrank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface KeywordSearchRankRepository extends JpaRepository<KeywordSearchRank, Long> {

    @Modifying
    @Query(value = "INSERT INTO keyword_search_ranks (keyword, search_count, week_start) " +
            "VALUES (:keyword, 1, :weekStart) " +
            "ON DUPLICATE KEY UPDATE search_count = search_count + 1",
            nativeQuery = true)
    void upsertSearchCount(@Param("keyword") String keyword, @Param("weekStart") LocalDate weekStart);

    List<KeywordSearchRank> findTop10ByWeekStartOrderBySearchCountDesc(LocalDate weekStart);

    List<KeywordSearchRank> findByWeekStartOrderBySearchCountDesc(LocalDate weekStart);
}