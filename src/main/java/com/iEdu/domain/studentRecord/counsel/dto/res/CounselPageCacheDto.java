package com.iEdu.domain.studentRecord.counsel.dto.res;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounselPageCacheDto {
    private List<CounselDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
}

