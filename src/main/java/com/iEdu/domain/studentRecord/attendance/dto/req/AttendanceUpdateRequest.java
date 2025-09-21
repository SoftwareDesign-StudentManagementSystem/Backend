package com.iEdu.domain.studentRecord.attendance.dto.req;

import com.iEdu.domain.studentRecord.attendance.entity.PeriodAttendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUpdateRequest{
    private List<PeriodAttendance> periodAttendances;
}
