package com.example.ainews.domain.author;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.ainews.domain.author.dto.AuthorRequest;
import com.example.ainews.domain.author.dto.AuthorResponse;

@RestController
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/api/providers/{providerId}/authors")
    public ResponseEntity<List<AuthorResponse>> findByProviderId(@PathVariable Integer providerId) {
        return ResponseEntity.ok(authorService.findByProviderId(providerId));
    }

    @PostMapping("/api/providers/{providerId}/authors")
    public ResponseEntity<AuthorResponse> create(@PathVariable Integer providerId,
                                                 @RequestBody AuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authorService.create(providerId, request));
    }

    @GetMapping("/api/authors/{id}")
    public ResponseEntity<AuthorResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(authorService.findById(id));
    }

    @PutMapping("/api/authors/{id}")
    public ResponseEntity<AuthorResponse> update(@PathVariable Integer id,
                                                 @RequestBody AuthorRequest request) {
        return ResponseEntity.ok(authorService.update(id, request));
    }

    @DeleteMapping("/api/authors/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
