package com.iEdu.domain.studentRecord.feedback.repository;

import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.global.common.enums.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // 피드백 조회
    Page<Feedback> findByMemberId(Long memberId, Pageable pageable);

    // 학생의 피드백 조회 및 권한 확인
    Page<Feedback> findByMemberIdAndVisibleToStudentTrue(Long memberId, Pageable pageable);

    // 학부모의 피드백 조회 및 권한 확인
    Page<Feedback> findByMemberIdAndVisibleToParentTrue(Long memberId, Pageable pageable);

    // 특정 학년/학기의 피드백 데이터 조회
    Page<Feedback> findByMemberIdAndYearAndSemester(Long memberId, Integer year, Semester semester, Pageable pageable);

    // 특정 학년/학기의 피드백 데이터 조회(보고서용)
    List<Feedback> findByMemberIdAndYearAndSemester(Long studentId, Integer year, Semester semester);

    // 특정 학년/학기의 피드백 데이터 조회 및 학생 권한 확인
    Page<Feedback> findByMemberIdAndYearAndSemesterAndVisibleToStudentTrue(
            Long memberId, Integer year, Semester semester, Pageable pageable
    );

    // 특정 학년/학기의 피드백 데이터 조회 및 학부모 권한 확인
    Page<Feedback> findByMemberIdAndYearAndSemesterAndVisibleToParentTrue(
            Long memberId, Integer year, Semester semester, Pageable pageable
    );
}
