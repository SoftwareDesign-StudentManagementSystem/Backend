package com.iEdu.domain.studentRecord.feedback.entity;

import jakarta.persistence.*;
import lombok.*;

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

    private String content;

    private Boolean visibleToStudent;

    private Boolean visibleToParent;
}


