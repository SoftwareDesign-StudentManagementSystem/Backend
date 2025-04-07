package com.iEdu.domain.studentRecord.specialty.dto.req;

import com.iEdu.domain.studentRecord.specialty.entity.SpecialtyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyForm {

    @NotNull(message = "카테고리는 필수입니다.")
    private SpecialtyCategory category;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;
}
