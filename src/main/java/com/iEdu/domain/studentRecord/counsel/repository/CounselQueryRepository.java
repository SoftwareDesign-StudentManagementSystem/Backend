package com.iEdu.domain.studentRecord.counsel.repository;

import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.global.common.enums.Semester;

import java.util.List;

public interface CounselQueryRepository {
    // 여러 학생 id를 한 번에 조회
    List<Counsel> findByMemberIdInAndYearAndSemester(List<Long> memberIds, Integer year, Semester semester);
}
