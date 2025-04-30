package com.iEdu.domain.notification.eventListener;

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
    private final FcmTokenService fcmTokenService; // 🔥 추가: 토큰 조회용

    @KafkaListener(topics = "grade-topic", groupId = "1")
    public void consume(String message) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                Notification notification = objectMapper.readValue(message, Notification.class);
                // 1. 학생 알림 생성 + FCM 전송
                notificationService.createNotification(notification);
                String studentToken = fcmTokenService.getFcmToken(notification.getReceiverId());
                if (studentToken != null) {
                    FcmMessage studentMessage = FcmMessage.builder()
                            .targetToken(studentToken)
                            .title("성적 알림")
                            .body(notification.getContent())
                            .build();
                    fcmService.sendMessageTo(studentMessage);
                } else {
                    log.warn("FCM token not found for studentId: {}", notification.getReceiverId());
                }
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
                    String parentToken = fcmTokenService.getFcmToken(parent.getId());
                    if (parentToken != null) {
                        FcmMessage parentMessage = FcmMessage.builder()
                                .targetToken(parentToken)
                                .title("자녀 성적 알림")
                                .body(notification.getContent())
                                .build();
                        fcmService.sendMessageTo(parentMessage);
                    } else {
                        log.warn("FCM token not found for parentId: {}", parent.getId());
                    }
                }
                return;
            } catch (Exception e) {
                attempt++;
                log.error("Retry attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        kafkaTemplate.send("grade-topic-dlt", message);
    }
}

