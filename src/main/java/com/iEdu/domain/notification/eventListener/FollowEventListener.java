package com.iEdu.domain.notification.eventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.notification.service.NotificationService;
import com.iEdu.domain.fcm.dto.FcmMessage;
import com.iEdu.domain.fcm.service.FcmService;
import com.iEdu.domain.fcm.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FollowEventListener {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService; // 🔥 추가: 토큰 조회용

    @KafkaListener(topics = "follow-topic", groupId = "1")
    public void consume(String message) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                Notification notification = objectMapper.readValue(message, Notification.class);
                notificationService.createNotification(notification);
                String fcmToken = fcmTokenService.getFcmToken(notification.getReceiverId());
                if (fcmToken != null) {
                    FcmMessage fcmMessage = FcmMessage.builder()
                            .targetToken(fcmToken)
                            .title("팔로우 알림")
                            .body(notification.getContent())
                            .build();
                    fcmService.sendMessageTo(fcmMessage);
                } else {
                    log.warn("FCM token not found for memberId: {}", notification.getReceiverId());
                }
                return;
            } catch (Exception e) {
                attempt++;
                log.error("Retry attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        kafkaTemplate.send("follow-topic-dlt", message);
    }
}
