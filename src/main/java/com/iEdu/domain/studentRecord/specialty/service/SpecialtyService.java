package com.iEdu.domain.studentRecord.specialty.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyForm;
import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyDto;
import com.iEdu.domain.studentRecord.specialty.entity.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SpecialtyService {
    // 학생의 모든 특기사항 조회 [학부모/선생님 권한]
    Page<SpecialtyDto> getAllSpecialty(Long studentId, Pageable pageable, LoginUserDto loginUser);

    // (학년/학기)로 학생 특기사항 조회 [학부모/선생님 권한]
    Page<SpecialtyDto> getFilterSpecialty(Long studentId, Integer year, Integer semester, Pageable pageable, LoginUserDto loginUser);

    // 학생 특기사항 생성 [선생님 권한]
    void createSpecialty(Long studentId, SpecialtyForm specialtyForm, LoginUserDto loginUser);

    // 학생 특기사항 수정 [선생님 권한]
    void updateSpecialty(Long specialtyId, SpecialtyForm specialtyForm, LoginUserDto loginUser);

    // 학생 특기사항 삭제 [선생님 권한]
    void deleteSpecialty(Long specialtyId, LoginUserDto loginUser);

    // Specialty → SpecialtyDto 변환
    SpecialtyDto convertToSpecialtyDto(Specialty specialty);
}
