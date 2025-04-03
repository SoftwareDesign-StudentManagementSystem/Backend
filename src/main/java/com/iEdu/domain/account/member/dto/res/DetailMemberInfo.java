package com.iEdu.domain.account.member.dto.res;

import com.iEdu.domain.account.member.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DetailMemberInfo {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private Member.Gender gender;  // 성별
    private LocalDate birthday;
    private String profileImgUrl;
}
