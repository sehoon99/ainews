package com.example.ainews.domain.subscriber.dto;

import com.example.ainews.domain.subscriber.Subscriber;

import java.time.LocalDateTime;

public record SubscribeResponse(Long id, String email, Boolean active, LocalDateTime createdAt) {

    public static SubscribeResponse from(Subscriber subscriber) {
        return new SubscribeResponse(
                subscriber.getId(),
                subscriber.getEmail(),
                subscriber.getActive(),
                subscriber.getCreatedAt()
        );
    }
}