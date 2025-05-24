package com.iEdu.global.initData.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdService {
    private final NotProdMemberService notProdMemberService;
    private final NotProdStudentRecordService notProdStudentRecordService;
    private final List<Long> studentIds = new ArrayList<>();
    private final List<String> studentPasswords = new ArrayList<>();
    private final List<Long> parentIds = new ArrayList<>();
    private final List<Long> teacherIds = new ArrayList<>();
    private final List<String> teacherPasswords = new ArrayList<>();
    List<Long> adminIdHolder = new ArrayList<>();
    List<String> adminPwHolder = new ArrayList<>();
    private Long adminAccountId;
    private String adminPassword;

    @Transactional
    public void initDummyData() {
        // 학생 가데이터 생성
        notProdMemberService.createStudents(studentIds, studentPasswords);

        // 학부모 가데이터 생성
        notProdMemberService.createParents(studentIds, studentPasswords, parentIds);

        // 선생님 가데이터 생성
        notProdMemberService.createTeachers(teacherIds, teacherPasswords);

        // 관리자 가데이터 생성
        notProdMemberService.createAdmin(adminIdHolder, adminPwHolder);
        this.adminAccountId = adminIdHolder.get(0);
        this.adminPassword = adminPwHolder.get(0);

        // 성적 가데이터 생성
        notProdStudentRecordService.createGradeData();

        // 출결 가데이터 생성
        notProdStudentRecordService.createAttendanceData();

        // 가데이터 정보 출력
        NotProdPrintTestAccount.printTestAccounts(
                studentIds,
                studentPasswords,
                parentIds,
                teacherIds,
                teacherPasswords,
                String.valueOf(adminAccountId),
                adminPassword
        );
    }
}
