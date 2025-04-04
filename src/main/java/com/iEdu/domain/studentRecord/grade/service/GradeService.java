package com.iEdu.domain.studentRecord.grade.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GradeService {
    // 선택한 학생의 성적 조회 [학생/학부모는 본인/자녀 성적 조회]
    Page<GradeDto> findByMemberId(Long studentId, LoginUserDto loginUser, Pageable pageable);
}
