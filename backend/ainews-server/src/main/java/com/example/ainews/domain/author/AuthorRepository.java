package com.example.ainews.domain.author;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Integer> {

    List<Author> findByProviderId(Integer providerId);
}
