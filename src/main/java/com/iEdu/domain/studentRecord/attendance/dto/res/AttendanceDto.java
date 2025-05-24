package com.iEdu.domain.studentRecord.attendance.dto.res;

import com.iEdu.global.common.enums.Semester;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AttendanceDto {
    private Long id;
    private Long studentId;
    private Integer year;
    private Semester semester;
    private LocalDate date;
    @Builder.Default
    private List<PeriodAttendanceDto> periodAttendanceDtos = new ArrayList<>();
}
