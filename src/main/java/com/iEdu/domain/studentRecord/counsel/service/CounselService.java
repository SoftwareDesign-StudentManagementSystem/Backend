package com.iEdu.domain.studentRecord.counsel.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselForm;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselDto;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CounselService {
    // 학생의 모든 상담 조회 [학부모/선생님 권한]
    Page<CounselDto> getAllCounsel(Long studentId, Pageable pageable, LoginUserDto loginUser);

    // (학년/반/번호/학기)로 학생들 상담 조회 [선생님 권한]
    List<CounselDto> getStudentsCounsel(Integer year, Integer classId, Integer number, Integer semester, LoginUserDto loginUser);

    // (학년/학기)로 학생 상담 조회 [학부모/선생님 권한]
    Page<CounselDto> getFilterCounsel(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser);

    // 학생 상담 생성 [선생님 권한]
    void createCounsel(Long studentId, CounselForm counselForm, LoginUserDto loginUser);

    // 학생 상담 수정 [선생님 권한]
    void updateCounsel(Long counselId, CounselForm counselForm, LoginUserDto loginUser);

    // 학생 상담 삭제 [선생님 권한]
    void deleteCounsel(Long counselId, LoginUserDto loginUser);

    // Counsel -> CounselDto 변환
    CounselDto convertToCounselDto(Counsel counsel);
}
