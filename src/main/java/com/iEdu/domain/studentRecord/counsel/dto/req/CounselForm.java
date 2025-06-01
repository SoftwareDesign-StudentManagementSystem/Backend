package com.iEdu.domain.studentRecord.counsel.dto.req;

import com.iEdu.global.common.enums.Semester;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounselForm {
    private Integer year;
    private Semester semester;
    private LocalDate date;
    private String content;
    private LocalDate nextCounselDate;
}
