package com.iEdu.domain.studentRecord.attendance.dto.res;

import com.iEdu.global.common.enums.Semester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDto {
    private Long id;
    private Long studentId;
    private Integer year;
    private Semester semester;
    private LocalDate date;
    @Builder.Default
    private List<PeriodAttendanceDto> periodAttendanceDtos = new ArrayList<>();
}
