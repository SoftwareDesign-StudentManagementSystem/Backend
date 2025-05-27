package com.iEdu.domain.studentRecord.counsel.dto.res;


import com.iEdu.global.common.enums.Semester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CounselDto {
    private Long id;
    private Long studentId;
    private Integer year;
    private Semester semester;
    private String content;
    private LocalDate nextCounselDate;
    private LocalDate date;
}
