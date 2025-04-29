package com.iEdu.domain.studentRecord.attendance.dto.req;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceForm {
    private Integer year;
    private Grade.Semester semester;
}
