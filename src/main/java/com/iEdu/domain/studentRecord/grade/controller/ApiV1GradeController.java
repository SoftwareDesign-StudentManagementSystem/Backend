package com.iEdu.domain.studentRecord.grade.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeDto;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.entity.GradePage;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/rest-api/v1/grade")
@RequiredArgsConstructor
public class ApiV1GradeController {
    private final GradeService gradeService;

    // 선택한 학생의 성적 조회 [학생/학부모는 본인/자녀 성적 조회]
    @GetMapping("/{studentId}")
    public ApiResponse<GradeDto> getGrade(@ModelAttribute GradePage request,
                                          @PathVariable("studentId") Long studentId, @LoginUser LoginUserDto loginUser) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(gradeService.findByMemberId(studentId, loginUser, pageable)));
    }

    // 학생 성적 필터링(학기)


    // 학생 성적 생성


    // 학생 성적 수정


    // 학생 성적 삭제


}
