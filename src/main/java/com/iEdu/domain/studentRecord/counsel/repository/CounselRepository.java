package com.iEdu.domain.studentRecord.counsel.repository;

import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.global.common.enums.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CounselRepository extends JpaRepository<Counsel, Long> {
    // 특정 회원의 상담 내역 전체 조회 (페이징)
    Page<Counsel> findByMemberId(Long memberId, Pageable pageable);

    // 특정 회원의 상담 내역 필터 조회 (학년 + 학기 + 페이징)
    Page<Counsel> findByMemberIdAndYearAndSemester(Long memberId, Integer year, Semester semester, Pageable pageable);

    // 특정 회원의 상담 내역 필터 조회 (학년 + 학기) - 페이징 없이
    List<Counsel> findByMemberIdAndYearAndSemester(Long memberId, Integer year, Semester semester);
}
