package com.iEdu.domain.studentRecord.specialty.entity;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Specialty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관된 학생(Member)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 카테고리 (예: 학업, 태도 등)
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private SpecialtyCategory category;

    // 특기사항 내용
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private Member writer;
}
