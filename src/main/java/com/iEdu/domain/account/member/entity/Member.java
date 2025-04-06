package com.iEdu.domain.account.member.entity;

import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Column(length = 50)
    private String schoolName;

    private Integer year;

    private Integer classId;

    private Integer number;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Subject subject;  // 담당과목(선생님만 해당)
    public enum Subject{
        KOREAN_LANGUAGE, MATHEMATICS, ENGLISH, SOCIAL_STUDIES, HISTORY, ETHICS, ECONOMICS, PHYSICS, CHEMISTRY, BIOLOGY,
        EARTH_SCIENCE, MUSIC, ART, PHYSICAL_EDUCATION, TECHNOLOGY_AND_HOME_ECONOMICS, COMPUTER_SCIENCE, SECOND_FOREIGN_LANGUAGE
        }

    @Enumerated(EnumType.STRING)
    @Column(length = 6)
    private Gender gender;  // 성별
    public enum Gender {
        MALE, FEMALE;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.ROLE_PARENT;  // 역할
    public enum MemberRole {
        ROLE_STUDENT, ROLE_TEACHER, ROLE_ADMIN, ROLE_PARENT
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 6, nullable = false)
    @Builder.Default
    private State state = State.NORMAL;  // 멤버 상태
    public enum State {
        NORMAL, BANNED
    }

    @OneToMany(mappedBy = "follow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberFollow> followList = new ArrayList<>();  // 자녀 리스트

    @OneToMany(mappedBy = "followed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberFollow> followedList = new ArrayList<>();  // 학부모 리스트

    @OneToMany(mappedBy = "followReq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberFollowReq> followReqList = new ArrayList<>();  // 팔로우 요청한 자녀 리스트

    @OneToMany(mappedBy = "followRec", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberFollowReq> followRecList = new ArrayList<>();  // 팔로우 요청 받은 학부모 리스트
}
