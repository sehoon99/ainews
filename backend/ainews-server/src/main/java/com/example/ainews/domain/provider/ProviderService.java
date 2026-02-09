package com.example.ainews.domain.provider;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainews.domain.provider.dto.ProviderRequest;
import com.example.ainews.domain.provider.dto.ProviderResponse;
import com.example.ainews.global.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ProviderService {

    private final ProviderRepository providerRepository;

    public ProviderService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    public List<ProviderResponse> findAll() {
        return providerRepository.findAll().stream()
                .map(ProviderResponse::from)
                .toList();
    }

    public ProviderResponse findById(Integer id) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));
        return ProviderResponse.from(provider);
    }

    @Transactional
    public ProviderResponse create(ProviderRequest request) {
        Provider provider = new Provider(request.name());
        return ProviderResponse.from(providerRepository.save(provider));
    }

    @Transactional
    public ProviderResponse update(Integer id, ProviderRequest request) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + id));
        provider.setName(request.name());
        return ProviderResponse.from(providerRepository.save(provider));
    }

    @Transactional
    public void delete(Integer id) {
        if (!providerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Provider not found: " + id);
        }
        providerRepository.deleteById(id);
    }
}
