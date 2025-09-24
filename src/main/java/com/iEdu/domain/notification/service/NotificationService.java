package com.iEdu.domain.notification.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.notification.dto.req.NotificationRequest;
import com.iEdu.domain.notification.dto.res.NotificationResponse;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    // 알림 생성 [선생님 권한]
    void createNotification(Notification notification);

    // 알림 목록 조회 [학부모/학생 권한]
    PageResponse<NotificationResponse> getNotifications(Pageable pageable, CurrentUserDto currentUser);

    // 알림 읽음 처리 [학부모/학생 권한]
    void markAsRead(NotificationRequest notificationRequest, CurrentUserDto currentUser);

    // Notification을 NotificationDto로 변환
    NotificationResponse convertToNotificationDto(Notification notification);
}
