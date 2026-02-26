package com.example.ainews.domain.subscriber;

import com.example.ainews.domain.subscriber.dto.SubscribeRequest;
import com.example.ainews.domain.subscriber.dto.SubscribeResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscribers")
public class SubscriberController {

    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping
    public ResponseEntity<SubscribeResponse> subscribe(@RequestBody SubscribeRequest request) {
        SubscribeResponse response = subscriberService.subscribe(request.email());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(@RequestParam String token) {
        try {
            subscriberService.unsubscribe(token);
            String html = """
                    <!DOCTYPE html>
                    <html lang="ko">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>구독 해제 - AI News Tracker</title>
                        <style>
                            body {
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                background-color: #0c1222;
                                color: #e4e4e7;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                min-height: 100vh;
                                margin: 0;
                            }
                            .card {
                                background-color: #111827;
                                border: 1px solid #1e2d4a;
                                border-radius: 16px;
                                padding: 48px;
                                text-align: center;
                                max-width: 400px;
                            }
                            h1 { font-size: 20px; margin-bottom: 12px; color: #f4f4f5; }
                            p { font-size: 14px; color: #a1a1aa; line-height: 1.6; }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <h1>구독이 해제되었습니다</h1>
                            <p>더 이상 AI News Tracker 키워드 변동 리포트를 받지 않습니다.<br>
                            언제든지 다시 구독할 수 있습니다.</p>
                        </div>
                    </body>
                    </html>
                    """;
            return ResponseEntity.ok(html);
        } catch (IllegalArgumentException e) {
            String errorHtml = """
                    <!DOCTYPE html>
                    <html lang="ko">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>오류 - AI News Tracker</title>
                        <style>
                            body {
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                background-color: #0c1222;
                                color: #e4e4e7;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                min-height: 100vh;
                                margin: 0;
                            }
                            .card {
                                background-color: #111827;
                                border: 1px solid #1e2d4a;
                                border-radius: 16px;
                                padding: 48px;
                                text-align: center;
                                max-width: 400px;
                            }
                            h1 { font-size: 20px; margin-bottom: 12px; color: #fca5a5; }
                            p { font-size: 14px; color: #a1a1aa; line-height: 1.6; }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <h1>유효하지 않은 링크입니다</h1>
                            <p>구독 해제 링크가 올바르지 않거나 이미 만료되었습니다.</p>
                        </div>
                    </body>
                    </html>
                    """;
            return ResponseEntity.badRequest().body(errorHtml);
        }
    }
}