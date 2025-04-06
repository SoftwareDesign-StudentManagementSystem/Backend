package com.iEdu.domain.studentRecord.grade.repository;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    // 멤버ID&학년&학기로 성적 조회
    Optional<Grade> findByMemberIdAndYearAndSemester(Long memberId, Integer year, Grade.Semester semester);

    // 멤버ID로 모든 성적 조회(학년&학기 내림차순)
    Page<Grade> findAllByMemberId(Long memberId, Pageable pageable);

    // 학년&학기로 성적 필터링
    List<Grade> findAllByMember_YearAndSemester(Integer year, Grade.Semester semester);

    // 멤버&학년&학기로 성적 조회
    Optional<Grade> findByMemberAndYearAndSemester(Member member, Integer year, Grade.Semester semester);
}
