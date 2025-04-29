package com.iEdu.domain.notification.entity;

import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {
    private Long receiverId;
    private Long objectId;
    private String content;

    @Builder.Default
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TargetObject targetObject;
    public enum TargetObject {
        Attendance, Counsel, Feedback, Grade, Member, Specialty
    }
}
