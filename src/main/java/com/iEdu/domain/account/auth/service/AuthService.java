package com.iEdu.domain.account.auth.service;

import com.iEdu.domain.account.auth.dto.req.LoginRequest;
import com.iEdu.domain.account.auth.dto.res.LoginResponse;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;

public interface AuthService {
    // 로그인
    LoginResponse login(LoginRequest loginRequest, boolean isSocialLogin);

    // 로그아웃
    void logout(LoginUserDto loginUser);

    // accessToken 재발급
    LoginResponse refreshToken(String refreshToken, LoginUserDto loginUser);
}
