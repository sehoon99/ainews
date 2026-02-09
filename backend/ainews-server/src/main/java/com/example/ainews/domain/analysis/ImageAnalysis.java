package com.example.ainews.domain.analysis;

import java.time.LocalDateTime;

import com.example.ainews.domain.article.Article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "image_analyses")
public class ImageAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "image_hash", nullable = false, length = 64)
    private String imageHash;

    @Column(name = "api_name", length = 50)
    private String apiName;

    @Column(name = "ai_probability")
    private Float aiProbability;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    protected ImageAnalysis() {
    }

    public ImageAnalysis(Article article, String imageUrl, String imageHash,
                         String apiName, Float aiProbability) {
        this.article = article;
        this.imageUrl = imageUrl;
        this.imageHash = imageHash;
        this.apiName = apiName;
        this.aiProbability = aiProbability;
        this.analyzedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Article getArticle() {
        return article;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public Float getAiProbability() {
        return aiProbability;
    }

    public void setAiProbability(Float aiProbability) {
        this.aiProbability = aiProbability;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }
}
