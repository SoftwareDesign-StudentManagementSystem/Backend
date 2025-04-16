package com.iEdu.domain.account.member.dto.req;

import com.iEdu.domain.account.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherUpdateForm {
    private String password;
    private String name;
    private String phone;
    private String email;
    private LocalDate birthday;
    private String schoolName;
    private Integer year;
    private Integer classId;
    private Member.Subject subject;
    private Member.Gender gender;
}
