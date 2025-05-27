package com.iEdu.domain.studentRecord.specialty.dto.req;

import com.iEdu.global.common.enums.Semester;
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
    private Integer year;
    private Semester semester;
    private String content;
}
