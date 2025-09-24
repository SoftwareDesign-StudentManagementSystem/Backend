package com.iEdu.domain.fcm.controller;

import com.iEdu.domain.account.auth.currentUser.CurrentUser;
import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.domain.fcm.dto.req.FcmTokenRequest;
import com.iEdu.domain.fcm.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest-api/v1/fcm")
@RequiredArgsConstructor
@Tag(name = "FCMToken", description = "FCM 토큰 API")
public class ApiV1FcmController {
    private final FcmTokenService fcmTokenService;

    // FCM Token 저장
    @Operation(summary = "FCM Token 저장")
    @PostMapping
    public ApiResponse<Void> saveFcmToken(@RequestBody @Valid FcmTokenRequest fcmTokenRequest, @CurrentUser CurrentUserDto currentUser) {
        fcmTokenService.saveFcmToken(currentUser.getId(), fcmTokenRequest.getFcmToken());
        return ApiResponse.success();
    }

    // FCM Token 삭제
    @Operation(summary = "FCM Token 삭제")
    @DeleteMapping
    public ApiResponse<Void> deleteFcmToken(@CurrentUser CurrentUserDto currentUser) {
        fcmTokenService.deleteFcmToken(currentUser.getId());
        return ApiResponse.success();
    }
}
