package com.iEdu.domain.studentRecord.feedback.entity;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Feedback extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Member member;

    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    private FeedbackCategory category;

    @Column(length = 1000)
    private String content;

    private Boolean visibleToStudent;
    private Boolean visibleToParent;
}
