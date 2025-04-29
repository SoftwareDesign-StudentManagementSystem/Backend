package com.iEdu.domain.studentRecord.attendance.entity;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
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
public class Attendance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Member member;

    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Grade.Semester semester;
    public enum Semester {
        FIRST_SEMESTER, SECOND_SEMESTER
    }
}
