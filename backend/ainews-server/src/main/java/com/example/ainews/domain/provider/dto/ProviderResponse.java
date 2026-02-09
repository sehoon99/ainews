package com.example.ainews.domain.provider.dto;

import java.time.LocalDateTime;

import com.example.ainews.domain.provider.Provider;

public record ProviderResponse(Integer id, String name, LocalDateTime createdAt) {

    public static ProviderResponse from(Provider provider) {
        return new ProviderResponse(provider.getId(), provider.getName(), provider.getCreatedAt());
    }
}
