package com.iEdu.domain.studentRecord.specialty.repository;

import com.iEdu.domain.studentRecord.specialty.entity.Specialty;
import com.iEdu.global.common.enums.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    // 본인의 모든 특기사항 조회
    Page<Specialty> findByMemberId(Long memberId, Pageable pageable);

    // (학년/학기)로 학생 특기사항 조회
    Page<Specialty> findByMemberIdAndYearAndSemester(Long memberId, Integer year, Semester semester, Pageable pageable);

    // (학년/학기)로 학생 특기사항 조회(보고서용)
    List<Specialty> findByMemberIdAndYearAndSemester(Long studentId, Integer year, Semester semester);
}
