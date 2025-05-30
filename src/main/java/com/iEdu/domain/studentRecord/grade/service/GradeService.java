package com.iEdu.domain.studentRecord.grade.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeForm;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeUpdateForm;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeDto;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GradeService {
    // 본인의 모든 성적 조회 [학생 권한]
    Page<GradeDto> getMyAllGrade(Pageable pageable, LoginUserDto loginUser);

    // 학생의 모든 성적 조회 [학부모/선생님 권한]
    Page<GradeDto> getAllGrade(Long studentId, Pageable pageable, LoginUserDto loginUser);

    // (학년/학기)로 본인 성적 조회 [학생 권한]
    GradeDto getMyFilterGrade(Integer year, Integer semester, LoginUserDto loginUser);

    // (학년/반/번호/학기)로 학생들 성적 조회 [선생님 권한]
    List<GradeDto> getStudentsGrade(Integer year, Integer classId, Integer number, Integer semester, LoginUserDto loginUser);

    // (학년/학기)로 학생 성적 조회 [학부모/선생님 권한]
    GradeDto getFilterGrade(Long studentId, Integer year, Integer semester, LoginUserDto loginUser);

    // 학생 성적 생성 [선생님 권한]
    void createGrade(Long studentId, GradeForm gradeForm, LoginUserDto loginUser);

    // 학생 성적 수정 [선생님 권한]
    void updateGrade(Long gradeId, GradeUpdateForm gradeUpdateForm, LoginUserDto loginUser);

    // 학생 성적 삭제 [선생님 권한]
    void deleteGrade(Long gradeId, LoginUserDto loginUser);

    // Grade -> GradeDto 변환
    GradeDto convertToGradeDto(Grade grade, Long studentAccountId);
}
