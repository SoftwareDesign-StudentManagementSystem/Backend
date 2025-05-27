package com.iEdu.domain.studentRecord.counsel.entity;

import lombok.Data;
import lombok.Getter;

@Data
public class CounselPage {
    // 기본 page, size
    private int page = 0;
    private int size = 15;
    @Getter
    private static final int maxPageSize = 15;
}
