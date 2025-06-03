package com.iEdu.domain.studentRecord.counsel.dto.res;

import com.iEdu.global.common.enums.Semester;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CounselDto {
    private Long id;
    private Long studentId;
    private String teacherName;
    private Integer year;
    private Semester semester;
    private LocalDate date;
    private String content;
    private LocalDate nextCounselDate;
}
