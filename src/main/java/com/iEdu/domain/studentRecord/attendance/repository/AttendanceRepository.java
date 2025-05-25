package com.iEdu.domain.studentRecord.attendance.repository;

import com.iEdu.domain.studentRecord.attendance.entity.Attendance;
import com.iEdu.global.common.enums.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    // 로그인한 유저 혹은 학생의 출결 전체 조회
    Page<Attendance> findByMemberId(Long memberId, Pageable pageable);

    // 특정 학년/학기의 출결 데이터 조회
    Page<Attendance> findAllByMemberIdAndYearAndSemester(Long memberId, Integer year, Semester semester, Pageable pageable);
}
