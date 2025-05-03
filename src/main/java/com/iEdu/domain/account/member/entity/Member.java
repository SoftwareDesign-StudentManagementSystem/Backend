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
    @Column(nullable = false)
    private Long accountId;

    @Column(length = 1000, nullable = false)
    private String password;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String email;

    @Column
    private LocalDate birthday;

    @Builder.Default
    @Column(length = 300)
    private String profileImageUrl = "";  // 프로필 사진 경로

    @Builder.Default
    private String schoolName = "송도고등학교";

    private Integer year;

    private Integer classId;

    private Integer number;

    @Enumerated(EnumType.STRING)
    @Column(length = 29)
    private Subject subject;  // 담당과목(선생님만 해당)
    public enum Subject{
        국어, 수학, 영어, 사회, 한국사, 윤리, 경제, 물리, 화학, 생명과학,
        지구과학, 음악, 미술, 체육, 기술가정, 컴퓨터, 제2외국어
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 6, nullable = false)
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

    @Builder.Default
    @OneToMany(mappedBy = "follow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberFollow> followList = new ArrayList<>();  // 자녀 리스트

    @Builder.Default
    @OneToMany(mappedBy = "followed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberFollow> followedList = new ArrayList<>();  // 학부모 리스트

    @Builder.Default
    @OneToMany(mappedBy = "followReq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberFollowReq> followReqList = new ArrayList<>();  // 팔로우 요청한 자녀 리스트

    @Builder.Default
    @OneToMany(mappedBy = "followRec", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberFollowReq> followRecList = new ArrayList<>();  // 팔로우 요청 받은 학부모 리스트
}
