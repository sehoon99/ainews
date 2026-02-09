package com.example.ainews.domain.search;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ainews.domain.article.Article;
import com.example.ainews.domain.analysis.ImageAnalysis;

public class ArticleDocument {

    private Long id;
    private String title;
    private String content;
    private String sourceUrl;
    private LocalDateTime publishedAt;
    private LocalDateTime crawledAt;
    private String portal;
    private String providerName;
    private String authorName;
    private boolean hasAiImages;
    private float maxAiProbability;
    private int imageCount;

    public ArticleDocument() {
    }

    public static ArticleDocument from(Article article) {
        ArticleDocument doc = new ArticleDocument();
        doc.id = article.getId();
        doc.title = article.getTitle();
        doc.content = article.getContent();
        doc.sourceUrl = article.getSourceUrl();
        doc.publishedAt = article.getPublishedAt();
        doc.crawledAt = article.getCrawledAt();
        doc.portal = article.getPortal();
        doc.providerName = article.getProvider() != null ? article.getProvider().getName() : null;
        doc.authorName = article.getAuthor() != null ? article.getAuthor().getName() : null;

        List<ImageAnalysis> analyses = article.getImageAnalyses();
        doc.imageCount = analyses.size();
        doc.maxAiProbability = 0f;
        doc.hasAiImages = false;

        if (!analyses.isEmpty()) {
            for (ImageAnalysis analysis : analyses) {
                if (analysis.getAiProbability() != null && analysis.getAiProbability() > doc.maxAiProbability) {
                    doc.maxAiProbability = analysis.getAiProbability();
                }
            }
            doc.hasAiImages = doc.maxAiProbability > 0.5f;
        }

        return doc;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }

    public String getPortal() {
        return portal;
    }

    public void setPortal(String portal) {
        this.portal = portal;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public boolean isHasAiImages() {
        return hasAiImages;
    }

    public void setHasAiImages(boolean hasAiImages) {
        this.hasAiImages = hasAiImages;
    }

    public float getMaxAiProbability() {
        return maxAiProbability;
    }

    public void setMaxAiProbability(float maxAiProbability) {
        this.maxAiProbability = maxAiProbability;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }
}
