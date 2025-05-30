package com.iEdu.domain.studentRecord.report.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ReportDto {
    private Map<String, String> reportUrls;
}
