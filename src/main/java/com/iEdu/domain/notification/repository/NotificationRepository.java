package com.iEdu.domain.notification.repository;

import com.iEdu.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // receiverId로 알림 조회
    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // notificationId와 receiverId로 알림 조회
    List<Notification> findAllByIdInAndReceiverId(List<Long> ids, Long receiverId);
}
