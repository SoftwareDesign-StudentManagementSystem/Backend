package com.iEdu.domain.notification.eventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.notification.dto.res.NotificationDto;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.notification.service.NotificationService;
import com.iEdu.domain.fcm.dto.FcmMessage;
import com.iEdu.domain.fcm.service.FcmService;
import com.iEdu.domain.fcm.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationEventListener {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;
    private final Map<String, NotificationMeta> notificationMetaMap = Map.of(
            "grade-topic", new NotificationMeta("성적 알림", "grade-topic-dlt"),
            "follow-topic", new NotificationMeta("팔로우 알림", "follow-topic-dlt"),
            "feedback-topic", new NotificationMeta("피드백 알림", "feedback-topic-dlt"),
            "counsel-topic", new NotificationMeta("상담 알림", "counsel-topic-dlt"),
            "specialty-topic", new NotificationMeta("특기사항 알림", "specialty-topic-dlt")
    );

    @KafkaListener(topics = {"grade-topic", "follow-topic", "feedback-topic", "counsel-topic", "specialty-topic"}, groupId = "1", concurrency = "3")
    public void consume(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String message = record.value();
        NotificationMeta meta = notificationMetaMap.get(topic);
        if (meta == null) {
            log.error("Unknown topic received: {}", topic);
            return;
        }
        handleMessage(message, meta.title(), meta.dltTopic());
    }

    private void handleMessage(String message, String title, String dltTopic) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                Notification notification = objectMapper.readValue(message, Notification.class);
                notificationService.createNotification(notification);
                String fcmToken = fcmTokenService.getFcmToken(notification.getReceiverId());
                if (fcmToken != null) {
                    NotificationDto notificationDto = notificationService.convertToNotificationDto(notification);
                    String bodyJson = objectMapper.writeValueAsString(notificationDto);

                    FcmMessage fcmMessage = FcmMessage.builder()
                            .targetToken(fcmToken)
                            .title(title)
                            .body(bodyJson)
                            .build();
                    fcmService.sendMessageTo(fcmMessage);
                } else {
                    log.warn("FCM token not found for receiverId: {}", notification.getReceiverId());
                }
                return;
            } catch (JsonProcessingException e) {
                log.error("Failed to parse message (not retryable): {}, error: {}", message, e.getMessage());
                break;
            } catch (Exception e) {
                attempt++;
                log.error("Retry attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        kafkaTemplate.send(dltTopic, message);
    }
    private record NotificationMeta(String title, String dltTopic) {}
}
