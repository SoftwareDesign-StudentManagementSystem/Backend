package com.iEdu.domain.studentRecord.attendance.dto.req;

import com.iEdu.domain.studentRecord.attendance.entity.PeriodAttendance;
import com.iEdu.global.common.enums.Semester;
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
    private Integer year;
    private Semester semester;
    private LocalDate date;
    private List<PeriodAttendance> periodAttendances;
}
