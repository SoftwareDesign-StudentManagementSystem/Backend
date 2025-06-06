package com.iEdu.domain.account.auth.security;

import com.iEdu.domain.account.auth.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 인증이 필요 없는 URL 리스트
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/rest-api/v1/auth/login") ||
                path.startsWith("/rest-api/v1/member/parent");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // OPTIONS 요청은 필터를 건너뜁니다
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String token = jwtTokenProvider.resolveToken(request);

        // JWT가 없을 경우
        if (!StringUtils.hasText(token)) {
            log.warn("JWT 토큰이 없습니다.");
            responseUnauthorized(response, "인증 실패");
            return;
        }

        boolean decodingSuccess = false;
        Long accountId = null;
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("토큰이 유효하지 않습니다.");
                responseUnauthorized(response, "유효하지 않은 Access Token 입니다.");
                return;
            }

            jwtTokenProvider.decodeToken(token);
            accountId = jwtTokenProvider.getAccountIdFromToken(token);
            decodingSuccess = true;
            log.info("정상적으로 사용자 정보를 토큰으로부터 가져왔습니다. AccountId: {}", accountId);
        } catch (Exception e) {
            log.warn("유효하지 않은 Access Token: {}", e.getMessage());
        }

        if (decodingSuccess) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(String.valueOf(accountId));
                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("SecurityContext 에 인증 정보 설정 완료: {}", authentication);
            } catch (Exception e) {
                log.error("인증 처리 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                refreshTokenService.deleteRefreshToken(String.valueOf(accountId));
            }
        }
        filterChain.doFilter(request, response);
    }

    private void responseUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }
}
