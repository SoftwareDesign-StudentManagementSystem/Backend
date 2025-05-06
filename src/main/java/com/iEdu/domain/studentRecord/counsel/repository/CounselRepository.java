package com.iEdu.domain.studentRecord.counsel.repository;

import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface CounselRepository extends JpaRepository<Counsel, Long> {
    @Query("""
        SELECT new com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse(
            c.date,
            t.name,
            c.content,
            c.visibleToStudent,
            c.visibleToParent
        )
        FROM Counsel c
        JOIN Member t ON c.teacherId = t.id
        WHERE c.studentId = :studentId
          AND c.date BETWEEN :startDate AND :endDate
          AND (:teacherName IS NULL OR t.name LIKE %:teacherName%)
        ORDER BY c.date DESC
    """)
    Page<CounselResponse> findCounselsByStudentAndFilter(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("teacherName") String teacherName,
            Pageable pageable
    );
    Page<Counsel> findByStudentIdAndDateBetween(Long studentId, LocalDate startDate, LocalDate endDate, Pageable pageable);

}
