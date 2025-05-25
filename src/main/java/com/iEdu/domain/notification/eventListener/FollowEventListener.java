package com.iEdu.domain.notification.eventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final FcmTokenService fcmTokenService;

    @KafkaListener(topics = "follow-topic", groupId = "1")
    public void consume(String message) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                Notification notification = objectMapper.readValue(message, Notification.class);
                // 알림 저장
                notificationService.createNotification(notification);
                // FCM 전송
                sendFcm(notification.getReceiverId(), "팔로우 알림", notification.getContent());
                return; // 성공하면 리턴
            } catch (JsonProcessingException e) {
                log.error("Failed to parse message (not retryable): {}, error: {}", message, e.getMessage());
                break; // 재시도 의미 없음
            } catch (Exception e) {
                attempt++;
                log.error("Retry attempt {} failed for message {}: {}", attempt, message, e.getMessage());
            }
        }
        kafkaTemplate.send("follow-topic-dlt", message); // 실패한 메시지 DLT로
    }

    private void sendFcm(Long receiverId, String title, String body) {
        String token = fcmTokenService.getFcmToken(receiverId);
        if (token != null) {
            FcmMessage message = FcmMessage.builder()
                    .targetToken(token)
                    .title(title)
                    .body(body)
                    .build();
            try {
                fcmService.sendMessageTo(message);
            } catch (Exception e) {
                log.error("Failed to send FCM to receiverId {}: {}", receiverId, e.getMessage());
            }
        } else {
            log.warn("FCM token not found for receiverId: {}", receiverId);
        }
    }
}
