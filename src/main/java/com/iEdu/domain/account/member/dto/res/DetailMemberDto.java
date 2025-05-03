package com.iEdu.domain.account.member.dto.res;

import com.iEdu.domain.account.member.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DetailMemberDto {
    private Long id;
    private Long accountId;
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
    private List<SimpleMember> childrenList;
    private List<SimpleMember> parentList;
    private List<SimpleMember> followReqList;
    private List<SimpleMember> followRecList;
}
