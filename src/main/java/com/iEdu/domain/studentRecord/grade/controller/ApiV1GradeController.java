package com.iEdu.domain.studentRecord.grade.controller;

import com.iEdu.domain.account.auth.loginUser.LoginUser;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeRequest;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeUpdateRequest;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeResponse;
import com.iEdu.domain.studentRecord.grade.entity.GradePage;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.ApiResponse;
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
    public ApiResponse<List<GradeResponse>> getMyAllGrade(@ModelAttribute GradePage request, @LoginUser LoginUserDto loginUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(gradeService.getMyAllGrade(pageable, loginUser));
    }

    // 학생의 모든 성적 조회 [학부모/선생님 권한]
    @Operation(summary = "학생의 모든 성적 조회 [학부모/선생님 권한]")
    @GetMapping("/{studentId}")
    public ApiResponse<List<GradeResponse>> getAllGrade(@ModelAttribute GradePage request,
                                                        @PathVariable("studentId") Long studentId,
                                                        @LoginUser LoginUserDto loginUser){
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return ApiResponse.success(gradeService.getAllGrade(studentId, pageable, loginUser));
    }

    // (학년/학기)로 본인 성적 조회 [학생 권한]
    @Operation(summary = "(학년/학기)로 본인 성적 조회 [학생 권한]")
    @GetMapping("/filter")
    public ApiResponse<GradeResponse> getMyFilterGrade(@RequestParam(value = "year") Integer year,
                                                       @RequestParam(value = "semester") Semester semester,
                                                       @LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(gradeService.getMyFilterGrade(year, semester, loginUser));
    }

    // (학년/학기)로 학생 성적 조회 [학부모/선생님 권한]
    @Operation(summary = "(학년/학기)로 학생 성적 조회 [학부모/선생님 권한]")
    @GetMapping("/filter/{studentId}")
    public ApiResponse<GradeResponse> getFilterGrade(@PathVariable("studentId") Long studentId,
                                                     @RequestParam(value = "year") Integer year,
                                                     @RequestParam(value = "semester") Semester semester,
                                                     @LoginUser LoginUserDto loginUser) {
        return ApiResponse.success(gradeService.getFilterGrade(studentId, year, semester, loginUser));
    }

    // (학년/반/번호/학기)로 학생들 성적 조회 [선생님 권한]
    @Operation(summary = "(학년/반/번호/학기)로 학생들 성적 조회 [선생님 권한]")
    @GetMapping("/filter/students")
    public ApiResponse<List<GradeResponse>> getStudentsGrade(@RequestParam(value = "year") Integer year,
                                                             @RequestParam(value = "classId") Integer classId,
                                                             @RequestParam(value = "number", required = false) Integer number,
                                                             @RequestParam(value = "semester") Semester semester,
                                                             @LoginUser LoginUserDto loginUser){
        return ApiResponse.success(gradeService.getStudentsGrade(year, classId, number, semester, loginUser));
    }

    // 학생 성적 생성 [선생님 권한]
    @Operation(summary = "학생 성적 생성 [선생님 권한]")
    @PostMapping("/{studentId}")
    public ApiResponse<Void> createGrade(@PathVariable("studentId") Long studentId,
                                           @RequestBody @Valid GradeRequest gradeRequest,
                                           @LoginUser LoginUserDto loginUser) {
        gradeService.createGrade(studentId, gradeRequest, loginUser);
        return ApiResponse.success();
    }

    // 학생 성적 수정 [선생님 권한]
    @Operation(summary = "학생 성적 수정 [선생님 권한]")
    @PatchMapping("/{gradeId}")
    public ApiResponse<Void> updateGrade(@PathVariable("gradeId") Long gradeId,
                                           @RequestBody @Valid GradeUpdateRequest gradeUpdateRequest,
                                           @LoginUser LoginUserDto loginUser){
        gradeService.updateGrade(gradeId, gradeUpdateRequest, loginUser);
        return ApiResponse.success();
    }

    // 학생 성적 삭제 [선생님 권한]
    @Operation(summary = "학생 성적 삭제 [선생님 권한]")
    @DeleteMapping("/{gradeId}")
    public ApiResponse<Void> deleteGrade(@PathVariable("gradeId") Long gradeId, @LoginUser LoginUserDto loginUser){
        gradeService.deleteGrade(gradeId, loginUser);
        return ApiResponse.success();
    }
}
