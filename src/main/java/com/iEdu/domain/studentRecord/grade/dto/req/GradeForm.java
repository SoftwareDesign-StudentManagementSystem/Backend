package com.iEdu.domain.studentRecord.grade.dto.req;

import com.iEdu.global.common.enums.Semester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeForm {
    private Integer year;
    private Semester semester;
    private Double score;
}
