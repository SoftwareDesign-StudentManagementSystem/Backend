package com.iEdu.domain.account.auth.loginUser;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberFollowReq;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class LoginUserDto {
    private Long id;
    private Long accountId;
    private String password;
    private String name;
    private String phone;
    private String email;
    private LocalDate birthday;
    private String profileImageUrl;
    private String schoolName;
    private Integer year;
    private Integer classId;
    private Integer number;
    private Member.Subject subject;
    private Member.Gender gender;
    private Member.MemberRole role;
    private Member.State state;
    private List<MemberFollow> followList;
    private List<MemberFollow> followedList;
    private List<MemberFollowReq> followReqList;
    private List<MemberFollowReq> followRecList;

    // Member 객체를 LoginUserDto로 변환하는 정적 팩토리 메서드
    public static LoginUserDto ConvertToLoginUserDto(Member member) {
        return LoginUserDto.builder()
                .id(member.getId())
                .accountId(member.getAccountId())
                .password(member.getPassword())
                .name(member.getName())
                .phone(member.getPhone())
                .email(member.getEmail())
                .birthday(member.getBirthday())
                .profileImageUrl(member.getProfileImageUrl())
                .schoolName(member.getSchoolName())
                .year(member.getYear())
                .classId(member.getClassId())
                .number(member.getNumber())
                .subject(member.getSubject())
                .gender(member.getGender())
                .role(member.getRole())
                .state(member.getState())
                .followList(member.getFollowList())
                .followedList(member.getFollowedList())
                .followReqList(member.getFollowReqList())
                .followRecList(member.getFollowRecList())
                .build();
    }

    // LoginUserDto를 Member 엔티티로 변환하는 메서드
    public Member ConvertToMember() {
        return Member.builder()
                .id(this.id)
                .accountId(this.accountId)
                .password(this.password)
                .name(this.name)
                .phone(this.phone)
                .email(this.email)
                .birthday(this.birthday)
                .profileImageUrl(this.profileImageUrl)
                .schoolName(this.schoolName)
                .year(this.year)
                .classId(this.classId)
                .number(this.number)
                .subject(this.subject)
                .gender(this.gender)
                .role(this.role)
                .state(this.state)
                .followList(this.followList)
                .followedList(this.followedList)
                .followReqList(this.followReqList)
                .followRecList(this.followRecList)
                .build();
    }
}
