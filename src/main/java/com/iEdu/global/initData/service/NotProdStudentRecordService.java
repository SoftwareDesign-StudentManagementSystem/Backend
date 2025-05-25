package com.iEdu.global.initData.service;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.attendance.dto.req.AttendanceForm;
import com.iEdu.domain.studentRecord.attendance.entity.PeriodAttendance;
import com.iEdu.domain.studentRecord.attendance.service.AttendanceService;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeForm;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.initData.utils.NotProdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdStudentRecordService {
    private final GradeService gradeService;
    private final AttendanceService attendanceService;
    private final MemberRepository memberRepository;
    private final NotProdUtils notProdUtils;

    // 성적 가데이터 생성
    public void createGradeData() {
        List<Member> teacherList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_TEACHER)
                .toList();
        List<Member.Subject> subjectList = List.of(Member.Subject.values());
        for (int studentGrade = 1; studentGrade <= 3; studentGrade++) {
            int startId = (studentGrade - 1) * 175 + 1;
            int endId = studentGrade * 175;

            for (int studentId = startId; studentId <= endId; studentId++) {
                for (int targetGrade = 1; targetGrade <= studentGrade; targetGrade++) {
                    int finalGrade = targetGrade;
                    List<Member> teachersForGrade = teacherList.stream()
                            .filter(t -> t.getYear() == finalGrade)
                            .collect(Collectors.toList());

                    for (int subjectIdx = 0; subjectIdx < subjectList.size(); subjectIdx++) {
                        Member teacher = teachersForGrade.get(subjectIdx);
                        LoginUserDto loginUser = LoginUserDto.ConvertToLoginUserDto(teacher);

                        for (Semester semester : Semester.values()) {
                            GradeForm gradeForm = GradeForm.builder()
                                    .year(finalGrade)
                                    .semester(semester)
                                    .score(notProdUtils.generateScore())
                                    .build();
                            gradeService.createGrade((long) studentId, gradeForm, loginUser);
                        }
                    }
                }
            }
        }
        System.out.println("-- 총 2,100개의 성적 데이터 생성 완료! --");
    }

    // 출결 가데이터 생성
    public void createAttendanceData() {
        List<Member> teacherList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_TEACHER)
                .toList();
        List<Member> studentList = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == Member.MemberRole.ROLE_STUDENT)
                .toList();

        for (Member teacher : teacherList) {
            Integer teacherYear = teacher.getYear();
            Integer teacherClassId = teacher.getClassId();

            // 본인 반 학생만 필터링
            List<Member> students = studentList.stream()
                    .filter(s -> Objects.equals(s.getYear(), teacherYear) &&
                            Objects.equals(s.getClassId(), teacherClassId))
                    .toList();
            LoginUserDto loginUser = LoginUserDto.ConvertToLoginUserDto(teacher);

            for (int targetYear = 1; targetYear <= teacherYear; targetYear++) {
                for (Semester semester : Semester.values()) {
                    List<LocalDate> schoolDays = notProdUtils.getSchoolDaysForSemester(semester);

                    for (Member student : students) {
                        // 출결 생성 시 년도는 targetYear(과거 학년)으로 세팅
                        for (LocalDate date : schoolDays) {
                            List<PeriodAttendance> periodAttendances = new ArrayList<>();

                            for (PeriodAttendance.Period period : PeriodAttendance.Period.values()) {
                                PeriodAttendance.State state = notProdUtils.generateRandomState();
                                periodAttendances.add(
                                        PeriodAttendance.builder()
                                                .period(period)
                                                .state(state)
                                                .build()
                                );
                            }
                            AttendanceForm form = AttendanceForm.builder()
                                    .studentId(student.getId())
                                    .year(targetYear)  // 출결은 과거 학년 단위로 생성
                                    .semester(semester)
                                    .date(date)
                                    .periodAttendances(periodAttendances)
                                    .build();
                            attendanceService.createAttendance(student.getId(), form, loginUser);
                        }
                    }
                }
            }
        }
        System.out.println("-- 약 178,500개의 출결 데이터 생성 완료! --");
    }
}
