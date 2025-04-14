package com.iEdu.domain.studentRecord.counsel.repository;

import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CounselRepository extends JpaRepository<Counsel, Long> {
    List<Counsel> findByStudentId(Long studentId);
}
