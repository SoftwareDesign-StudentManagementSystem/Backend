package com.iEdu.domain.studentRecord.specialty.entity;

import lombok.Data;
import lombok.Getter;

@Data
public class SpecialtyPage {
    // 기본 페이지 번호와 사이즈 설정
    private int page = 0;
    private int size = 6;

    @Getter
    private static final int maxPageSize = 6;
}
