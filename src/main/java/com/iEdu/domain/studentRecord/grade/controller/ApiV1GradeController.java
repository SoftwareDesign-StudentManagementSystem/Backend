package com.iEdu.domain.studentRecord.grade.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeForm;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeUpdateForm;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeDto;
import com.iEdu.domain.studentRecord.grade.entity.GradePage;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.common.response.ApiResponse;
import com.iEdu.global.common.response.IEduPage;
import com.iEdu.global.exception.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/rest-api/v1/grade")
@RequiredArgsConstructor
@Tag(name = "Grade", description = "성적 API")
public class ApiV1GradeController {
    private final GradeService gradeService;

    // 본인의 모든 성적 조회 [학생 권한]
    @Operation(summary = "본인의 모든 성적 조회 [학생 권한]")
    @GetMapping
    public ApiResponse<GradeDto> getMyAllGrade(@ModelAttribute GradePage request, @LoginUser LoginUserDto loginUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(gradeService.getMyAllGrade(pageable, loginUser)));
    }

    // 학생의 모든 성적 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 성적 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<GradeDto> getAllGrade(@ModelAttribute GradePage request,
                                             @PathVariable("studentId") Long studentId,
                                             @LoginUser LoginUserDto loginUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.of(IEduPage.of(gradeService.getAllGrade(studentId, pageable, loginUser)));
    }

    // (학년/학기)로 본인 성적 조회 [학생 권한]
    @Operation(summary = "(학년/학기)로 본인 성적 조회 [학생 권한]")
    @GetMapping("/filter")
    public ApiResponse<GradeDto> getMyFilterGrade(@RequestParam(value = "year") Integer year,
                                                  @RequestParam(value = "semester") Integer semester,
                                                  @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(gradeService.getMyFilterGrade(year, semester, loginUser));
    }

    // (학년/반/번호/학기)로 학생들 성적 조회 [선생님 권한]
    @Operation(summary = "(학년/반/번호/학기)로 학생들 성적 조회 [선생님 권한]")
    @GetMapping("/filter/students")
    public ApiResponse<List<GradeDto>> getStudentsGrade(@RequestParam(value = "year") Integer year,
                                                        @RequestParam(value = "classId") Integer classId,
                                                        @RequestParam(value = "number", required = false) Integer number,
                                                        @RequestParam(value = "semester") Integer semester,
                                                        @LoginUser LoginUserDto loginUser){
        return ApiResponse.of(gradeService.getStudentsGrade(year, classId, number, semester, loginUser));
    }

    // (학년/학기)로 학생 성적 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 성적 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<GradeDto> getFilterGrade(@PathVariable("studentId") Long studentId,
                                                @RequestParam(value = "year") Integer year,
                                                @RequestParam(value = "semester") Integer semester,
                                                @LoginUser LoginUserDto loginUser) {
        return ApiResponse.of(gradeService.getFilterGrade(studentId, year, semester, loginUser));
    }

    // 학생 성적 생성 [선생님 권한]
    @Operation(summary = "학생 성적 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<String> createGrade(@PathVariable("studentId") Long studentId,
                                           @RequestBody @Valid GradeForm gradeForm,
                                           @LoginUser LoginUserDto loginUser) {
        gradeService.createGrade(studentId, gradeForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 성적 수정 [선생님 권한]
    @Operation(summary = "학생 성적 수정 [선생님 권한]")
    @PutMapping("/{gradeId}")
    public ApiResponse<String> updateGrade(@PathVariable("gradeId") Long gradeId,
                                           @RequestBody @Valid GradeUpdateForm gradeUpdateForm,
                                           @LoginUser LoginUserDto loginUser){
        gradeService.updateGrade(gradeId, gradeUpdateForm, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    // 학생 성적 삭제 [선생님 권한]
    @Operation(summary = "학생 성적 삭제 [선생님 권한]")
    @DeleteMapping("/{gradeId}")
    public ApiResponse<String> deleteGrade(@PathVariable("gradeId") Long gradeId, @LoginUser LoginUserDto loginUser){
        gradeService.deleteGrade(gradeId, loginUser);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }
}
