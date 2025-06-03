package com.iEdu.domain.notification.serviceImpl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberPage;
import com.iEdu.domain.notification.dto.req.NotificationForm;
import com.iEdu.domain.notification.dto.res.NotificationDto;
import com.iEdu.domain.notification.dto.res.NotificationPageCacheDto;
import com.iEdu.domain.notification.entity.Notification;
import com.iEdu.domain.notification.repository.NotificationRepository;
import com.iEdu.domain.notification.service.NotificationService;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.iEdu.global.common.utils.RoleValidator.validateStudentOrParentRole;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private final ObjectMapper objectMapper;

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
        checkPageSize(pageable.getPageSize());
        // ROLE_STUDENT, ROLE_PARENT가 아닌 경우 예외 처리
        validateStudentOrParentRole(loginUser);

        // 캐시 키 생성 (사용자 ID, 역할, 페이지 번호, 페이지 크기 포함)
        String cacheKey = String.format("notification:%d:%s:%d:%d",
                loginUser.getId(),
                loginUser.getRole().name(),
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        // Redis에서 캐시 조회
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            JavaType type = objectMapper.getTypeFactory().constructType(NotificationPageCacheDto.class);
            NotificationPageCacheDto cacheDto = objectMapper.convertValue(cached, type);
            return new PageImpl<>(
                    cacheDto.getContent(),
                    PageRequest.of(cacheDto.getPageNumber(), cacheDto.getPageSize()),
                    cacheDto.getTotalElements()
            );
        }
        // DB에서 조회
        Page<Notification> notificationPage = notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(loginUser.getId(), pageable);
        Page<NotificationDto> resultPage = notificationPage.map(this::convertToNotificationDto);
        // 캐시에 저장 (TTL 10분)
        NotificationPageCacheDto cacheDto = NotificationPageCacheDto.builder()
                .content(resultPage.getContent())
                .pageNumber(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .build();
        redisTemplate.opsForValue().set(cacheKey, cacheDto, Duration.ofMinutes(10));
        return resultPage;
    }

    // 알림 읽음 처리 [학부모/학생 권한]
    @Override
    @Transactional
    public void markAsRead(NotificationForm notificationForm, LoginUserDto loginUser) {
        // ROLE_STUDENT, ROLE_PARENT가 아닌 경우 예외 처리
        validateStudentOrParentRole(loginUser);
        // 본인 알림인지 확인
        List<Long> ids = notificationForm.getNotificationIdList();
        List<Notification> notifications = notificationRepository.findAllByIdInAndReceiverId(ids, loginUser.getId());
        if (notifications.size() != ids.size()) {
            throw new ServiceException(ReturnCode.NOTIFICATION_NOT_FOUND);
        }
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
        // 캐시 무효화
        evictNotificationCache(loginUser.getId());
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = MemberPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    public void evictNotificationCache(Long userId) {
        String pattern = "notification:" + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // Notification을 NotificationDto로 변환
    public NotificationDto convertToNotificationDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .objectId(notification.getObjectId())
                .targetObject(notification.getTargetObject())
                .build();
    }
}
