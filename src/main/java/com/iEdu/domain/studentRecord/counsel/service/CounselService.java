package com.iEdu.domain.studentRecord.counsel.service;


import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;

import java.util.List;

public interface CounselService {
    void addCounsel(CounselRequest request);
    List<CounselResponse> getCounselsByStudent(Long studentId);
}
