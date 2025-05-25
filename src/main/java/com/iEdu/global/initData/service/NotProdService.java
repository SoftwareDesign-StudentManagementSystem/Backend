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

    // 1) 트랜잭션 내 데이터 생성 메서드
    @Transactional
    public void initDummyDataTransactional() {
        notProdMemberService.createStudents(studentIds, studentPasswords);
        notProdMemberService.createParents(studentIds, studentPasswords, parentIds);
        notProdMemberService.createTeachers(teacherIds, teacherPasswords);
        notProdMemberService.createAdmin(adminIdHolder, adminPwHolder);
        this.adminAccountId = adminIdHolder.get(0);
        this.adminPassword = adminPwHolder.get(0);
        notProdStudentRecordService.createGradeData();
        notProdStudentRecordService.createAttendanceData();
        // 알림 등 이벤트는 트랜잭션 커밋 이후 처리됨
    }

    // 2) 트랜잭션 커밋 후 가데이터 정보 출력
    public void initDummyData() {
        initDummyDataTransactional();
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
