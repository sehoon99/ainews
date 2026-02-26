package com.example.ainews.domain.keywordstat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface KeywordStatRepository extends JpaRepository<KeywordStat, Long> {

    List<KeywordStat> findByStatDateOrderByRankPositionAsc(LocalDate statDate);

    @Modifying
    @Query("DELETE FROM KeywordStat ks WHERE ks.statDate < :cutoffDate")
    void deleteByStatDateBefore(@Param("cutoffDate") LocalDate cutoffDate);
}