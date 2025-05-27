package com.iEdu.domain.studentRecord.grade.repository;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.global.common.enums.Semester;

import java.util.List;

public interface GradeQueryRepository {
    List<Grade> findAllByStudentInfoAndSemesterAndYear(
            Integer studentYear, Integer classId, Integer number,
            Semester semester);
}
