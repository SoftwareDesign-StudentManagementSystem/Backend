package com.iEdu.domain.notification.eventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.notification.service.NotificationService;
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

    @KafkaListener(topics = "grade-topic", groupId = "1")
    public void consume(String message) {
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                Notification notification = objectMapper.readValue(message, Notification.class);
                // 1. 학생 알림 생성
                notificationService.createNotification(notification);
                List<Member> parentList = memberService.findParentsByStudentId(notification.getReceiverId());
                // 2. 학부모 알림 생성
                for (Member parent : parentList) {
                    Notification parentNotification = Notification.builder()
                            .receiverId(parent.getId()) // 학부모 id로 저장
                            .objectId(notification.getObjectId())
                            .content(notification.getContent())
                            .targetObject(notification.getTargetObject())
                            .build();
                    notificationService.createNotification(parentNotification);
                }
                return; // 성공하면 빠져나가기
            } catch (Exception e) {
                attempt++;
                log.error("Retry attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        // 3번 실패했으면 DLT로
        kafkaTemplate.send("grade-topic-dlt", message);
    }
}
