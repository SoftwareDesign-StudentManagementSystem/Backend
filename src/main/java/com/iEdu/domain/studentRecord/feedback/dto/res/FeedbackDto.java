package com.iEdu.domain.studentRecord.feedback.dto.res;

import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackDto {
    private String teacher;
    private FeedbackCategory category;
    private String content;
    private Boolean visibleToStudent;
    private Boolean visibleToParent;
}
