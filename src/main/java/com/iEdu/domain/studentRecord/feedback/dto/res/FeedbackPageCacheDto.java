package com.iEdu.domain.studentRecord.feedback.dto.res;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackPageCacheDto {
    private List<FeedbackDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
}
