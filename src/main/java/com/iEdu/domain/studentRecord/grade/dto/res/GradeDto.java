package com.iEdu.domain.studentRecord.grade.dto.res;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeDto {
    private Long id;
    private Long studentId;
    private String profileImageUrl;
    private Integer year;
    private Grade.Semester semester;
    private SubjectScore koreanLanguage;
    private SubjectScore mathematics;
    private SubjectScore english;
    private SubjectScore socialStudies;
    private SubjectScore history;
    private SubjectScore ethics;
    private SubjectScore economics;
    private SubjectScore physics;
    private SubjectScore chemistry;
    private SubjectScore biology;
    private SubjectScore earthScience;
    private SubjectScore music;
    private SubjectScore art;
    private SubjectScore physicalEducation;
    private SubjectScore technologyAndHomeEconomics;
    private SubjectScore computerScience;
    private SubjectScore secondForeignLanguage;
}
