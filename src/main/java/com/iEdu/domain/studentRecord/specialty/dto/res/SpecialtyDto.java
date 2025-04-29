package com.iEdu.domain.studentRecord.specialty.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyDto {

    private Long id; // 특기사항 ID
    private Long memberId; // 작성 대상 학생의 멤버 ID
    private String content; // 특기사항 내용
    private String writerName; // 작성자 이름
    private LocalDate createdDate; // 생성일
}