package com.iEdu.domain.studentRecord.counsel.repository;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface CounselRepository extends JpaRepository<Counsel, Long> {
    Page<CounselResponse> getCounsels(Long studentId, Pageable pageable,
                                      LoginUserDto loginUser,
                                      LocalDate startDate, LocalDate endDate,
                                      String teacherName);
    Page<Counsel> findByStudentIdAndDateBetween(Long studentId, LocalDate startDate, LocalDate endDate, Pageable pageable);

}
