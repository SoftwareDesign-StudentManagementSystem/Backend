package com.iEdu.domain.studentRecord.feedback.entity;

import lombok.Data;
import lombok.Getter;

@Data
public class FeedbackPage {
    // 기본 page, size
    private int page = 0;
    private int size = 24;
    @Getter
    private static final int maxPageSize = 24;
}
