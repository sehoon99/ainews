package com.example.ainews.infra.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SesClient sesClient;
    private final String senderEmail;

    public EmailService(SesClient sesClient,
                        @Value("${ainews.ses.sender-email}") String senderEmail) {
        this.sesClient = sesClient;
        this.senderEmail = senderEmail;
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(senderEmail)
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().charset("UTF-8").data(subject).build())
                            .body(Body.builder()
                                    .html(Content.builder().charset("UTF-8").data(htmlBody).build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("이메일 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {} - {}", to, e.getMessage());
        }
    }
}