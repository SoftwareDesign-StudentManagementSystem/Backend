package com.iEdu.domain.studentRecord.specialty.service;

import com.iEdu.domain.account.auth.currentUser.CurrentUserDto;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyRequest;
import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyResponse;
import com.iEdu.domain.studentRecord.specialty.entity.Specialty;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface SpecialtyService {
    // 학생의 모든 특기사항 조회 [학부모/선생님 권한]
    PageResponse<SpecialtyResponse> getAllSpecialty(Long studentId, Pageable pageable, CurrentUserDto currentUser);

    // (학년/학기)로 학생 특기사항 조회 [학부모/선생님 권한]
    PageResponse<SpecialtyResponse> getFilterSpecialty(Long studentId, Integer year, Semester semester, Pageable pageable, CurrentUserDto currentUser);

    // 학생 특기사항 생성 [선생님 권한]
    void createSpecialty(Long studentId, SpecialtyRequest specialtyRequest, CurrentUserDto currentUser);

    // 학생 특기사항 수정 [선생님 권한]
    void updateSpecialty(Long specialtyId, SpecialtyRequest specialtyRequest, CurrentUserDto currentUser);

    // 학생 특기사항 삭제 [선생님 권한]
    void deleteSpecialty(Long specialtyId, CurrentUserDto currentUser);

    // Specialty → SpecialtyDto 변환
    SpecialtyResponse convertToSpecialtyDto(Specialty specialty);
}
