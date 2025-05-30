package com.iEdu.domain.studentRecord.report.dto.req;

import com.iEdu.global.common.enums.ReportFormat;
import com.iEdu.global.common.enums.Semester;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportForm {
    @Column(columnDefinition = "jsonb") // PostgreSQL의 jsonb 타입 사용
    @Size(max = 25, message = "최대 25명까지만 선택할 수 있습니다.")
    private List<Long> studentIdList;
    private Integer year;
    private Semester semester;
    private ReportFormat reportFormat;
    private Boolean grade;
    private Boolean attendance;
    private Boolean counsel;
    private Boolean feedback;
    private Boolean specialty;

    public ReportForm copyWithOnly(String field) {
        return ReportForm.builder()
                .studentIdList(this.studentIdList)
                .year(this.year)
                .semester(this.semester)
                .reportFormat(this.reportFormat)
                .grade("성적".equals(field))
                .attendance("출결".equals(field))
                .counsel("상담".equals(field))
                .feedback("피드백".equals(field))
                .specialty("특기사항".equals(field))
                .build();
    }
}
