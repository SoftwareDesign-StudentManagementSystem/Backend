package com.iEdu.domain.studentRecord.feedback.dto.req;

import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackForm {
    private Long studentId;
    private Long teacherId;
    private FeedbackCategory category;
    private String content;
    private Boolean visibleToStudent;
    private Boolean visibleToParent;
}
