package com.iEdu.domain.studentRecord.counsel.dto.res;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class CounselResponse {
    private LocalDate date;
    private String teacher;
    private String content;
    private boolean visibleToStudent;
    private boolean visibleToParent;
}
