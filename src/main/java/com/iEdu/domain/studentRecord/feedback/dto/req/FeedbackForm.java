package com.iEdu.domain.studentRecord.feedback.dto.req;

import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackForm {
    private FeedbackCategory category; // 피드백 카테고리
    private String content;          // 피드백 내용
    private Boolean visibleToStudent; // 학생에게 공개 여부
    private Boolean visibleToParent;  // 학부모에게 공개 여부
}
