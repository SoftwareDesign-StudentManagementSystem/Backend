package com.iEdu.domain.studentRecord.counsel.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CounselService {
    // 학생의 모든 상담 조회 [학부모/선생님 권한]
    PageResponse<CounselResponse> getAllCounsel(Long studentId, Pageable pageable, CurrentUserDto currentUser);

    // (학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]
    List<CounselResponse> getStudentsCounsel(Integer year, Integer classId, Integer number, Semester semester, CurrentUserDto currentUser);

    // (학년/학기)로 학생 상담 조회 [학부모/선생님 권한]
    PageResponse<CounselResponse> getFilterCounsel(Long studentId, Integer year, Semester semester, Pageable pageable, CurrentUserDto currentUser);

    // 학생 상담 생성 [선생님 권한]
    void createCounsel(Long studentId, CounselRequest counselRequest, CurrentUserDto currentUser);

    // 학생 상담 수정 [선생님 권한]
    void updateCounsel(Long counselId, CounselRequest counselRequest, CurrentUserDto currentUser);

    // 학생 상담 삭제 [선생님 권한]
    void deleteCounsel(Long counselId, CurrentUserDto currentUser);

    // Counsel -> CounselDto 변환
    CounselResponse convertToCounselDto(Counsel counsel);
}
