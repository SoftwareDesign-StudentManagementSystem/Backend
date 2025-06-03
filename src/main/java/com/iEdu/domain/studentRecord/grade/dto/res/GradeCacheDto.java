package com.iEdu.domain.studentRecord.grade.dto.res;

import com.iEdu.global.common.enums.Semester;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeCacheDto {
    private GradeDto gradeDto;
    private Integer year;
    private Semester semester;
}
