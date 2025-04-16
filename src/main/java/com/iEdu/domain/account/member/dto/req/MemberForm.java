package com.iEdu.domain.account.member.dto.req;

import com.iEdu.domain.account.member.entity.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberForm {
    @NotNull(message = "accountId는 필수입니다.")
    private Long accountId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String phone;

    private String email;

    @NotNull(message = "생일은 필수입니다.")
    private LocalDate birthday;

    @NotBlank(message = "학교 이름은 필수입니다.")
    private String schoolName;

    private Integer year;

    private Integer classId;

    private Integer number;

    private Member.Subject subject;

    @NotNull(message = "성별은 필수입니다.")
    private Member.Gender gender;

    @NotNull(message = "역할은 필수입니다.")
    private Member.MemberRole role;

    @NotNull(message = "상태는 필수입니다.")
    private Member.State state;
}
