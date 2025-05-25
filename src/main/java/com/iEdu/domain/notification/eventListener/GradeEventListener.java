package com.iEdu.domain.notification.eventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.service.MemberService;
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

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GradeEventListener {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MemberService memberService;
    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;

    @KafkaListener(topics = "grade-topic", groupId = "1", concurrency = "3")
    public void consume(String message) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                Notification notification = objectMapper.readValue(message, Notification.class);

                // 1. 학생 알림 생성 + FCM 전송
                notificationService.createNotification(notification);
                sendFcm(notification.getReceiverId(), "성적 알림", notification.getContent());

                // 2. 학부모 알림 생성 + FCM 전송
                List<Member> parentList = memberService.findParentsByStudentId(notification.getReceiverId());
                for (Member parent : parentList) {
                    Notification parentNotification = Notification.builder()
                            .receiverId(parent.getId())
                            .objectId(notification.getObjectId())
                            .content(notification.getContent())
                            .targetObject(notification.getTargetObject())
                            .build();
                    notificationService.createNotification(parentNotification);
                    sendFcm(parent.getId(), "자녀 성적 알림", notification.getContent());
                }
                return; // 성공 시 종료
            } catch (JsonProcessingException e) {
                log.error("Failed to parse message (not retryable): {}, error: {}", message, e.getMessage());
                break; // 재시도 무의미 → DLT 이동
            } catch (Exception e) {
                attempt++;
                log.error("Retry attempt {} failed for message {}: {}", attempt, message, e.getMessage());
            }
        }
        kafkaTemplate.send("grade-topic-dlt", message);
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
