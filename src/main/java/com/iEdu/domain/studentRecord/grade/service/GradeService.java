package com.iEdu.domain.studentRecord.grade.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeRequest;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeUpdateRequest;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeResponse;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GradeService {
    // 본인의 모든 성적 조회 [학생 권한]
    PageResponse<GradeResponse> getMyAllGrade(Pageable pageable, CurrentUserDto currentUser);

    // 학생의 모든 성적 조회 [학부모/선생님 권한]
    PageResponse<GradeResponse> getAllGrade(Long studentId, Pageable pageable, CurrentUserDto currentUser);

    // (학년/학기)로 본인 성적 조회 [학생 권한]
    GradeResponse getMyFilterGrade(Integer year, Semester semester, CurrentUserDto currentUser);

    // (학년/학기)로 학생 성적 조회 [학부모/선생님 권한]
    GradeResponse getFilterGrade(Long studentId, Integer year, Semester semester, CurrentUserDto currentUser);

    // (학년/반/번호/학기)로 학생들 성적 조회 [선생님 권한]
    List<GradeResponse> getStudentsGrade(Integer year, Integer classId, Integer number, Semester semester, CurrentUserDto currentUser);

    // 학생 성적 생성 [선생님 권한]
    void createGrade(Long studentId, GradeRequest gradeRequest, CurrentUserDto currentUser);

    // 학생 성적 수정 [선생님 권한]
    void updateGrade(Long gradeId, GradeUpdateRequest gradeUpdateRequest, CurrentUserDto currentUser);

    // 학생 성적 삭제 [선생님 권한]
    void deleteGrade(Long gradeId, CurrentUserDto currentUser);

    // Grade -> GradeDto 변환
    GradeResponse convertToGradeDto(Grade grade, Long studentAccountId, List<Grade> allGradesForYearAndSemester);
}
