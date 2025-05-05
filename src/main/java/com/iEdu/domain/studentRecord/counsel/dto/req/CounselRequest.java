package com.iEdu.domain.studentRecord.counsel.dto.req;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class CounselRequest {
    private Long studentId;
    private LocalDate date;
    private String content;
    private boolean visibleToStudent;
    private boolean visibleToParent;
}
