package com.example.ainews.domain.author;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainews.domain.author.dto.AuthorRequest;
import com.example.ainews.domain.author.dto.AuthorResponse;
import com.example.ainews.domain.provider.Provider;
import com.example.ainews.domain.provider.ProviderRepository;
import com.example.ainews.global.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final ProviderRepository providerRepository;

    public AuthorService(AuthorRepository authorRepository, ProviderRepository providerRepository) {
        this.authorRepository = authorRepository;
        this.providerRepository = providerRepository;
    }

    public List<AuthorResponse> findByProviderId(Integer providerId) {
        return authorRepository.findByProviderId(providerId).stream()
                .map(AuthorResponse::from)
                .toList();
    }

    public AuthorResponse findById(Integer id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + id));
        return AuthorResponse.from(author);
    }

    @Transactional
    public AuthorResponse create(Integer providerId, AuthorRequest request) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerId));
        Author author = new Author(provider, request.name(), request.email());
        return AuthorResponse.from(authorRepository.save(author));
    }

    @Transactional
    public AuthorResponse update(Integer id, AuthorRequest request) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + id));
        author.setName(request.name());
        author.setEmail(request.email());
        return AuthorResponse.from(authorRepository.save(author));
    }

    @Transactional
    public void delete(Integer id) {
        if (!authorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Author not found: " + id);
        }
        authorRepository.deleteById(id);
    }
}
