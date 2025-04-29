package com.iEdu.domain.notification.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.notification.dto.req.NotificationForm;
import com.iEdu.domain.notification.dto.res.NotificationDto;
import com.iEdu.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    // 알림 생성 [선생님 권한]
    void createNotification(Notification notification);

    // 알림 목록 조회 [학부모/학생 권한]
    Page<NotificationDto> getNotifications(Pageable pageable, LoginUserDto loginUser);

    // 알림 읽음 처리 [학부모/학생 권한]
    void markAsRead(NotificationForm notificationForm, LoginUserDto loginUser);
}
