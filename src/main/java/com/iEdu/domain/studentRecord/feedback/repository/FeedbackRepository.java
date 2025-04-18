package com.iEdu.domain.studentRecord.feedback.repository;

import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByStudentId(Long studentId);
}
