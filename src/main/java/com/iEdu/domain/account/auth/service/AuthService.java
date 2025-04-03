package com.iEdu.domain.account.auth.service;

import com.iEdu.domain.account.auth.dto.req.LoginForm;
import com.iEdu.domain.account.auth.dto.res.Auth;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;

public interface AuthService {
    // 로그인
    Auth login(LoginForm loginForm, boolean isSocialLogin);

    // 로그아웃
    void logout(LoginUserDto loginUser);

    // accessToken 재발급
    Auth refreshToken(String refreshToken, LoginUserDto loginUser);
}
