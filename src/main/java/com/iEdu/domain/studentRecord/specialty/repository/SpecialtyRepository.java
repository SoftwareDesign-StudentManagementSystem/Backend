package com.iEdu.domain.studentRecord.specialty.repository;

import com.iEdu.domain.studentRecord.specialty.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    // 특정 멤버의 모든 특기사항 조회 (최근순)
    List<Specialty> findAllByMemberIdOrderByIdDesc(Long memberId);
}
