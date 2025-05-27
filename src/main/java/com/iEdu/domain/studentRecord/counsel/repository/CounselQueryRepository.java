package com.iEdu.domain.studentRecord.counsel.repository;

import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.global.common.enums.Semester;

import java.util.List;

public interface CounselQueryRepository {
    List<Counsel> findByMemberIdAndYearAndSemester(Long memberId, Integer year, Semester semester);
}
