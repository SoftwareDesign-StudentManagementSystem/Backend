package com.iEdu.domain.studentRecord.attendance.entity;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private Semester semester;

    private LocalDate date;

    @OneToMany(mappedBy = "attendance", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PeriodAttendance> periodAttendances = new ArrayList<>();
}
