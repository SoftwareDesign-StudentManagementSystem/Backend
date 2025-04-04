package com.iEdu.domain.studentRecord.grade.repository;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    // 선택한 학생의 성적 조회 [학생/학부모는 본인/자녀 성적 조회]
    @Query("SELECT c FROM Grade c WHERE c.member.id = :studentId ORDER BY c.createdAt DESC")
    Page<Grade> findByMemberId(@Param("studentId") Long studentId, Pageable pageable);

    // 학년/학기로 성적 필터링
    List<Grade> findAllByMember_YearAndSemester(int year, Grade.Semester semester);
}
