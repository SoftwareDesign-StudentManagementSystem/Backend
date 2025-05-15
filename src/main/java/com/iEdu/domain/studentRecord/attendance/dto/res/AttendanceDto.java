package com.iEdu.domain.studentRecord.attendance.dto.res;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AttendanceDto {
    private Long id;
    private Long studentId;
    private Integer year;
    private Grade.Semester semester;
    private LocalDate date;
    private List<PeriodAttendanceDto> periodAttendances;
}
