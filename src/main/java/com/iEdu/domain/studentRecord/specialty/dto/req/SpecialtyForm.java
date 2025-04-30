package com.iEdu.domain.studentRecord.specialty.dto.req;

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
public class SpecialtyForm {
    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    private LocalDate recordedDate;
}
