package com.iEdu.domain.account.auth.serviceImpl;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String accountIdStr) throws UsernameNotFoundException {
        Long accountId;
        try {
            accountId = Long.parseLong(accountIdStr);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("올바르지 않은 accountId입니다: " + accountIdStr);
        }
        Member member = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + accountId));
        return org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(member.getAccountId()))
                .password(member.getPassword()) // 패스워드는 보통 사용 안 함 (JWT 기반 인증)
                .authorities("ROLE_USER") // 권한 설정 (추후 DB에서 가져올 수 있음)
                .build();
    }
}
