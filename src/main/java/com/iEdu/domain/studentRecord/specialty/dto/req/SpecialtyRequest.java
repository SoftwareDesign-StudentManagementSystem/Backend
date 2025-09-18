package com.iEdu.domain.studentRecord.specialty.dto.req;

import com.iEdu.global.common.enums.Semester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyRequest {
    private Integer year;
    private Semester semester;
    private LocalDate date;
    private String content;
}
