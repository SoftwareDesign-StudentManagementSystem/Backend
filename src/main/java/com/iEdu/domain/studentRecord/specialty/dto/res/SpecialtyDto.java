package com.iEdu.domain.studentRecord.specialty.dto.res;

import com.iEdu.domain.studentRecord.specialty.entity.SpecialtyCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyDto {

    private Long id; // 특기사항 ID
    private Long memberId; // 작성 대상 학생의 멤버 ID
    private SpecialtyCategory category; // ENUM - 카테고리 (예: 학업, 태도 등)
    private String content; // 특기사항 내용
}