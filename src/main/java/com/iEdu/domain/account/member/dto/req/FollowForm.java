package com.iEdu.domain.account.member.dto.req;

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
public class FollowForm {
    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "학년은 필수입니다.")
    private Integer year;

    @NotNull(message = "반은 필수입니다.")
    private Integer classId;

    @NotNull(message = "번호는 필수입니다.")
    private Integer number;

    @NotNull(message = "생일은 필수입니다.")
    private LocalDate birthday;
}
