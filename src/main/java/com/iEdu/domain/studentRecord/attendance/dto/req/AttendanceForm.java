package com.iEdu.domain.studentRecord.attendance.dto.req;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceForm {
    private Long studentId;
    private LocalDate date;
    private Integer year;
    private Grade.Semester semester;
    private List<PeriodAttendanceForm> periodAttendances;
}
