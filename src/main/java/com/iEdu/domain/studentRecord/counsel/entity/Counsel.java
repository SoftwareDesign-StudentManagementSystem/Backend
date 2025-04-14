package com.iEdu.domain.studentRecord.counsel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "counsel")
@Getter
@Setter
public class Counsel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Long teacherId;

    private LocalDate date;

    @Column(length = 1000)
    private String content;

    private Boolean visibleToStudent;
    private Boolean visibleToParent;
}
