package com.iEdu.domain.account.auth.loginUser;

import com.iEdu.domain.account.member.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LoginUserDto {
    private Long id;
    private String name;
    private String nickname;
    private String phone;
    private String email;
    private String password;
    private Member.Gender gender;
    private Member.State state;
    private Member.MemberRole role;
    private String profileImageUrl;
    private LocalDate birthday;

    // Member 객체를 LoginUserDto로 변환하는 정적 팩토리 메서드
    public static LoginUserDto ConvertToLoginUserDto(Member member) {
        return LoginUserDto.builder()
                .id(member.getId())
                .name(member.getName())
                .phone(member.getPhone())
                .email(member.getEmail())
                .password(member.getPassword())
                .gender(member.getGender())
                .state(member.getState())
                .role(member.getRole())
                .profileImageUrl(member.getProfileImageUrl())
                .birthday(member.getBirthday())
                .build();
    }

    // LoginUserDto를 Member 엔티티로 변환하는 메서드
    public Member ConvertToMember() {
        return Member.builder()
                .id(this.id)
                .name(this.name)
                .phone(this.phone)
                .email(this.email)
                .password(this.password)
                .gender(this.gender)
                .state(this.state)
                .role(this.role)
                .profileImageUrl(this.profileImageUrl)
                .birthday(this.birthday)
                .build();
    }
}
