package com.iEdu.domain.studentRecord.specialty.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyForm;
import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyDto;

import java.util.List;

public interface SpecialtyService {

    void createSpecialty(Long studentId, SpecialtyForm form, LoginUserDto loginUser);

    void updateSpecialty(Long specialtyId, SpecialtyForm form, LoginUserDto loginUser);

    // 특기사항 학년/학기별 조회 [학부모/선생님 권한]

    void deleteSpecialty(Long specialtyId, LoginUserDto loginUser);

    List<SpecialtyDto> getAllSpecialties(Long studentId, LoginUserDto loginUser);

    SpecialtyDto getSpecialty(Long specialtyId, LoginUserDto loginUser);
}
