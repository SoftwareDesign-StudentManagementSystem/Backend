package com.iEdu.domain.studentRecord.attendance.repository;

import com.iEdu.domain.studentRecord.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
}
