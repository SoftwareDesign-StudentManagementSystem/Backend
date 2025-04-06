package com.iEdu.domain.studentRecord.grade.entity;

import lombok.Data;
import lombok.Getter;

@Data
public class GradePage {
    // 기본 page, size
    private int page = 0;
    private int size = 6;
    @Getter
    private static final int maxPageSize = 6;
}
