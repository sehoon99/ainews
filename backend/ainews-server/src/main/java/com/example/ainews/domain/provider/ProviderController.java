package com.example.ainews.domain.provider;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ainews.domain.provider.dto.ProviderRequest;
import com.example.ainews.domain.provider.dto.ProviderResponse;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponse>> findAll() {
        return ResponseEntity.ok(providerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(providerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> create(@RequestBody ProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProviderResponse> update(@PathVariable Integer id,
                                                   @RequestBody ProviderRequest request) {
        return ResponseEntity.ok(providerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        providerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
