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
public class ParentForm {
    @NotNull(message = "accountId는 필수입니다.")
    private Long accountId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String phone;

    private String email;

    private LocalDate birthday;

    private String schoolName;

    @NotNull(message = "성별은 필수입니다.")
    private Member.Gender gender;
}
