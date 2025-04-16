package com.iEdu.domain.account.member.dto.res;

import com.iEdu.domain.account.member.entity.Member;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberDto {
    private Long id;
    private String name;
    private String profileImageUrl;
    private String schoolName;
    private Integer year;
    private Integer classId;
    private Integer number;
    private Member.Subject subject;
    private Member.MemberRole role;
}
