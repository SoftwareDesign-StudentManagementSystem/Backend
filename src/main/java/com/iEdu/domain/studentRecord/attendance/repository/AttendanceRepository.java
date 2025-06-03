package com.iEdu.domain.studentRecord.attendance.repository;

import com.iEdu.domain.studentRecord.attendance.entity.Attendance;
import com.iEdu.global.common.enums.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    // 로그인한 유저 혹은 학생의 출결 전체 조회
    Page<Attendance> findByMemberId(Long memberId, Pageable pageable);

    // 특정 학년/학기의 출결 데이터 조회
    Page<Attendance> findAllByMemberIdAndYearAndSemester(Long memberId, Integer year, Semester semester, Pageable pageable);

    // 특정 학년/학기의 출결 데이터 조회(보고서용)
    List<Attendance> findByMemberIdAndYearAndSemesterOrderByDateAsc(Long memberId, Integer year, Semester semester);

    // 특정 학년/학기/월의 출결 데이터 조회
    @Query("SELECT a FROM Attendance a WHERE a.member.id = :memberId " +
            "AND a.year = :year " +
            "AND a.semester = :semester " +
            "AND EXTRACT(MONTH FROM a.date) = :month")
    Page<Attendance> findAllByMemberIdAndYearAndSemesterAndMonth(
            @Param("memberId") Long memberId,
            @Param("year") Integer year,
            @Param("semester") Semester semester,
            @Param("month") Integer month,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT a.* FROM attendance a
        WHERE a.student_id = :memberId
        AND a.year = :year
        AND a.semester = :semester
        AND (:month IS NULL OR EXTRACT(MONTH FROM a.date) = :month)
        ORDER BY a.date ASC
    """,
            countQuery = """
        SELECT COUNT(*) FROM attendance a
        WHERE a.student_id = :memberId
        AND a.year = :year
        AND a.semester = :semester
        AND (:month IS NULL OR EXTRACT(MONTH FROM a.date) = :month)
    """,
            nativeQuery = true
    )
    Page<Attendance> findFilteredAttendancesByMemberAndYearAndSemesterAndOptionalMonth(
            @Param("memberId") Long memberId,
            @Param("year") Integer year,
            @Param("semester") String semester,
            @Param("month") Integer month,
            Pageable pageable
    );
}
