package com.iEdu.domain.studentRecord.grade.controller;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/rest-api/v1/grade")
@RequiredArgsConstructor
public class ApiV1GradeController {
    private final GradeService gradeService;

    // 선택한 학생의 성적 조회
//    @GetMapping
//    public ApiResponse<Grade> getGrade() {
//
//    }

    // 학생의 성적 검색(이름 & 반 & 번호)


    // 학생 성적 생성


    // 학생 성적 수정


    // 학생 성적 삭제

}
