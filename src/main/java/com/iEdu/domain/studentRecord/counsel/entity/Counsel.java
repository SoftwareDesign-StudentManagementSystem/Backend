package com.iEdu.domain.studentRecord.counsel.entity;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Counsel extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Member member;

    private String teacherName;

    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Semester semester;

    @Column(length = 1000)
    private String content;

    private LocalDate nextCounselDate;
}
