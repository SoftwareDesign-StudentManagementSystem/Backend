package com.iEdu.domain.notification.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.notification.dto.req.NotificationForm;
import com.iEdu.domain.notification.dto.res.NotificationDto;
import com.iEdu.domain.notification.entity.NotificationPage;
import com.iEdu.domain.notification.service.NotificationService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/notification")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 API")
public class ApiV1NotificationController {
    private final NotificationService notificationService;

    // 알림 목록 조회 [학부모/학생 권한]
    @Operation(summary = "알림 목록 조회 [학부모/학생 권한]")
    @GetMapping
    public ApiResponse<NotificationDto> getNotifications(@ModelAttribute NotificationPage request, @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(notificationService.getNotifications(pageable, loginUser)));
    }

    // 알림 읽음 처리 [학부모/학생 권한]
    @Operation(summary = "알림 읽음 처리 [학부모/학생 권한]")
    @PutMapping
    public ApiResponse<String> markAsRead(@RequestBody @Valid NotificationForm notificationForm,
                                          @LoginUser LoginUserDto loginUser) {
        notificationService.markAsRead(notificationForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
