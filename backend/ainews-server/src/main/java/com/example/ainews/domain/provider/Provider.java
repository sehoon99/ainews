package com.example.ainews.domain.provider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.ainews.domain.article.Article;
import com.example.ainews.domain.author.Author;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "provider")
    private List<Author> authors = new ArrayList<>();

    @OneToMany(mappedBy = "provider")
    private List<Article> articles = new ArrayList<>();

    protected Provider() {
    }

    public Provider(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public List<Article> getArticles() {
        return articles;
    }
}
