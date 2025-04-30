package com.iEdu.domain.studentRecord.feedback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Long teacherId;

    @Enumerated(EnumType.STRING)
    private FeedbackCategory category;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDate recordedDate;

    private Boolean visibleToStudent;
    private Boolean visibleToParent;
}


