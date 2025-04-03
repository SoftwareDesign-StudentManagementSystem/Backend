package com.iEdu.domain.account.member.entity;

import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@DynamicInsert
public class Member extends BaseEntity {
    private Long accountId;

    @Column(length = 1000)
    private String password;

    @Column(length = 20)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String email;

    private LocalDate birthday;

    @Column(length = 300)
    private String profileImageUrl = "";  // 프로필 사진 경로

    private Long grade;

    private Long classId;

    private Long number;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Subject subject;  // 담당과목(선생님만 해당)
    public enum Subject{
        KOREAN_LANGUAGE, MATHEMATICS, ENGLISH, SOCIAL_STUDIES, HISTORY, ETHICS, ECONOMICS, PHYSICS, CHEMISTRY, BIOLOGY,
        EARTH_SCIENCE, MUSIC, ART, PHYSICAL_EDUCATION, TECHNOLOGY, HOME_ECONOMICS, COMPUTER_SCIENCE
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 6)
    private Gender gender;  // 성별
    public enum Gender {
        MALE, FEMALE;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private MemberRole role;  // 역할
    public enum MemberRole {
        ROLE_STUDENT, ROLE_TEACHER, ROLE_ADMIN
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 6, nullable = false)
    @Builder.Default
    private State state = State.NORMAL;  // 멤버 상태
    public enum State {
        NORMAL, BANNED
    }
}
