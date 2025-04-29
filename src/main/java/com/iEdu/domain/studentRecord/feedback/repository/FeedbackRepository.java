package com.iEdu.domain.studentRecord.feedback.repository;

import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByStudentId(Long studentId);

    @Query("SELECT f FROM Feedback f " +
            "WHERE f.studentId = :studentId " +
            "AND f.recordedDate BETWEEN :startDate AND :endDate " +
            "AND (:teacherId IS NULL OR f.teacherId = :teacherId) " +
            "AND (:category IS NULL OR f.category = :category)")
    List<Feedback> searchByFilters(@Param("studentId") Long studentId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("teacherId") Long teacherId,
                                   @Param("category") FeedbackCategory category);

}
