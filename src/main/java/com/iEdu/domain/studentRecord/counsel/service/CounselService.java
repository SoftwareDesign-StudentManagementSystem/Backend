package com.iEdu.domain.studentRecord.counsel.service;


import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface CounselService {
    void addCounsel(CounselRequest request, LoginUserDto loginUser);
    Page<CounselResponse> getCounsels(Long studentId, Pageable pageable,
                                      LoginUserDto loginUser,
                                      LocalDate startDate, LocalDate endDate,
                                      String teacherName);
}
