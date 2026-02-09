package com.example.ainews.domain.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageAnalysisRepository extends JpaRepository<ImageAnalysis, Long> {

    List<ImageAnalysis> findByArticleId(Long articleId);

    List<ImageAnalysis> findByImageHash(String imageHash);
}
