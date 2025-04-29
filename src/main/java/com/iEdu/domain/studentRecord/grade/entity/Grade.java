package com.iEdu.domain.studentRecord.grade.entity;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Grade extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Member member;

    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Semester semester;
    public enum Semester {
        FIRST_SEMESTER, SECOND_SEMESTER;

        public String toKoreanString() {
            return switch (this) {
                case FIRST_SEMESTER -> "1학기";
                case SECOND_SEMESTER -> "2학기";
            };
        }
    }

    private Double koreanLanguageScore;
    private Double mathematicsScore;
    private Double englishScore;
    private Double socialStudiesScore;
    private Double historyScore;
    private Double ethicsScore;
    private Double economicsScore;
    private Double physicsScore;
    private Double chemistryScore;
    private Double biologyScore;
    private Double earthScienceScore;
    private Double musicScore;
    private Double artScore;
    private Double physicalEducationScore;
    private Double technologyAndHomeEconomicScore;
    private Double computerScienceScore;
    private Double secondForeignLanguageScore;
}
