package com.iEdu.domain.fcm.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.domain.fcm.dto.req.FcmToken;
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
    public ApiResponse<String> saveFcmToken(@RequestBody @Valid FcmToken fcmToken, @LoginUser LoginUserDto loginUser) {
        fcmTokenService.saveFcmToken(loginUser.getId(), fcmToken.getFcmToken());
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // FCM Token 삭제
    @Operation(summary = "FCM Token 삭제")
    @DeleteMapping
    public ApiResponse<String> deleteFcmToken(@LoginUser LoginUserDto loginUser) {
        fcmTokenService.deleteFcmToken(loginUser.getId());
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
