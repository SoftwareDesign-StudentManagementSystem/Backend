package com.iEdu.domain.studentRecord.specialty.dto.res;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyPageCacheDto {
    private List<SpecialtyDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
}
