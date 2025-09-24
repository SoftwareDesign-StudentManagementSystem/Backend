package com.iEdu.domain.account.auth.currentUser;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberFollowReq;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CurrentUserDto {
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
}
