package com.iEdu.domain.studentRecord.grade.repository;

import com.iEdu.domain.studentRecord.grade.entity.Grade;

import java.util.List;

public interface GradeQueryRepository {
    List<Grade> findAllByStudentInfoAndSemesterAndYear(
            Integer studentYear, Integer classId, Integer number,
            Grade.Semester semester, Integer gradeYear);
}
