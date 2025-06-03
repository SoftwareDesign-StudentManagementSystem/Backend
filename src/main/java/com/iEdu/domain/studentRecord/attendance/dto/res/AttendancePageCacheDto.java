package com.iEdu.domain.studentRecord.attendance.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePageCacheDto {
    private List<AttendanceDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
}
