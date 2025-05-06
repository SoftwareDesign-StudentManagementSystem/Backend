package com.iEdu.domain.notification.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.notification.dto.req.NotificationForm;
import com.iEdu.domain.notification.dto.res.NotificationDto;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.notification.repository.NotificationRepository;
import com.iEdu.domain.notification.service.NotificationService;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    // 알림 생성 [선생님 권한]
    @Override
    @Transactional
    public void createNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    // 알림 목록 조회 [학부모/학생 권한]
    @Override
    @Transactional
    public Page<NotificationDto> getNotifications(Pageable pageable, LoginUserDto loginUser) {
        // ROLE_STUDENT, ROLE_PARENT가 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT &&
                loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        checkPageSize(pageable.getPageSize());
        Page<Notification> notificationPage = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(loginUser.getId(), pageable);
        return notificationPage.map(this::convertToNotificationDto);
    }

    // 알림 읽음 처리 [학부모/학생 권한]
    @Override
    @Transactional
    public void markAsRead(NotificationForm notificationForm, LoginUserDto loginUser) {
        // ROLE_STUDENT, ROLE_PARENT가 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT &&
                loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        // 본인 알림인지 확인
        List<Long> ids = notificationForm.getNotificationIdList();
        List<Notification> notifications = notificationRepository.findAllByIdInAndReceiverId(ids, loginUser.getId());
        if (notifications.size() != ids.size()) {
            throw new ServiceException(ReturnCode.NOTIFICATION_NOT_FOUND);
        }
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // Notification을 NotificationDto로 변환
    private NotificationDto convertToNotificationDto(Notification notification){
        return new NotificationDto(
                notification.getId(),
                notification.getContent(),
                notification.getIsRead(),
                notification.getObjectId(),
                notification.getTargetObject()
        );
    }
}
