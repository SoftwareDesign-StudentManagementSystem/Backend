package com.iEdu.domain.studentRecord.attendance.dto.res;

import com.iEdu.domain.studentRecord.attendance.entity.PeriodAttendance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PeriodAttendanceDto {
    private Long AttendanceId;
    private PeriodAttendance.State state;
    private PeriodAttendance.Period period;
}
