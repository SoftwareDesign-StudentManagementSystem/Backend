package com.iEdu.domain.notification.controller;

import com.iEdu.domain.account.auth.currentUser.CurrentUser;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.notification.dto.req.NotificationRequest;
import com.iEdu.domain.notification.dto.res.NotificationResponse;
import com.iEdu.domain.notification.entity.NotificationPage;
import com.iEdu.domain.notification.service.NotificationService;
import com.iEdu.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest-api/v1/notification")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 API")
public class ApiV1NotificationController {
    private final NotificationService notificationService;

    // 알림 목록 조회 [학부모/학생 권한]
    @Operation(summary = "알림 목록 조회 [학부모/학생 권한]")
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(@ModelAttribute NotificationPage request, @CurrentUser CurrentUserDto currentUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(notificationService.getNotifications(pageable, currentUser));
    }

    // 알림 읽음 처리 [학부모/학생 권한]
    @Operation(summary = "알림 읽음 처리 [학부모/학생 권한]")
    @PatchMapping
    public ApiResponse<Void> markAsRead(@RequestBody @Valid NotificationRequest notificationRequest,
                                          @CurrentUser CurrentUserDto currentUser) {
        notificationService.markAsRead(notificationRequest, currentUser);
        return ApiResponse.success();
    }
}
