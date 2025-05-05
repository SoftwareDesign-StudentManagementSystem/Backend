package com.iEdu.domain.studentRecord.counsel.entity;

import lombok.Getter;

@Getter
public class CounselPage {
    // 기본 page, size
    private int page = 0;
    private int size = 6;
    private static final int maxPageSize = 6;
}
