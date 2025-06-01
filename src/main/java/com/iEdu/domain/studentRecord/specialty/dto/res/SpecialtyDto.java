package com.iEdu.domain.studentRecord.specialty.dto.res;

import com.iEdu.global.common.enums.Semester;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SpecialtyDto {
    private Long id; // 특기사항 ID
    private Long studentId; // 작성 대상 학생의 멤버 ID
    private String teacherName;
    private Integer year;
    private Semester semester;
    private LocalDate date;
    private String content;
}
