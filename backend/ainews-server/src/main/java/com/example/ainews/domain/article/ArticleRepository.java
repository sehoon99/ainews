package com.example.ainews.domain.article;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByProviderId(Integer providerId);

    @Query("SELECT DISTINCT a FROM Article a JOIN FETCH a.provider LEFT JOIN FETCH a.author WHERE a.crawledAt > :since ORDER BY a.crawledAt ASC")
    Slice<Article> findForIndexingSince(@Param("since") LocalDateTime since, Pageable pageable);
}
