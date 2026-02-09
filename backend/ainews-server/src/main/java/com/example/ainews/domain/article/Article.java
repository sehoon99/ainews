package com.example.ainews.domain.article;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.ainews.domain.analysis.ImageAnalysis;
import com.example.ainews.domain.author.Author;
import com.example.ainews.domain.provider.Provider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    @Column
    private String portal;

    @Column(nullable = false, length = 500)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "source_url", nullable = false, unique = true, length = 500)
    private String sourceUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    @OneToMany(mappedBy = "article")
    @org.hibernate.annotations.BatchSize(size = 100)
    private List<ImageAnalysis> imageAnalyses = new ArrayList<>();

    protected Article() {
    }

    public Article(Provider provider, Author author, String title, String content,
                   String sourceUrl, LocalDateTime publishedAt) {
        this.provider = provider;
        this.author = author;
        this.title = title;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.publishedAt = publishedAt;
        this.crawledAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
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

    public String getPortal() {
        return portal;
    }

    public void setPortal(String portal) {
        this.portal = portal;
    }

    public List<ImageAnalysis> getImageAnalyses() {
        return imageAnalyses;
    }
}
