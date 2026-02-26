package com.example.ainews.domain.subscriber;

import com.example.ainews.domain.subscriber.dto.SubscribeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    public SubscriberService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    @Transactional
    public SubscribeResponse subscribe(String email) {
        Subscriber subscriber = subscriberRepository.findByEmail(email)
                .map(existing -> {
                    if (existing.getActive()) {
                        throw new IllegalStateException("이미 구독 중인 이메일입니다.");
                    }
                    existing.activate();
                    return existing;
                })
                .orElseGet(() -> subscriberRepository.save(new Subscriber(email)));

        return SubscribeResponse.from(subscriber);
    }

    @Transactional
    public void unsubscribe(String token) {
        Subscriber subscriber = subscriberRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 구독 해제 링크입니다."));
        subscriber.deactivate();
    }

    public List<Subscriber> getActiveSubscribers() {
        return subscriberRepository.findAllByActiveTrue();
    }
}