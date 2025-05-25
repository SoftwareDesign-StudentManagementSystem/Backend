package com.iEdu.global.initData.service;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.req.ParentForm;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.global.initData.utils.NotProdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotProdMemberService {
    private final AdminService adminService;
    private final MemberService memberService;
    private final NotProdUtils notProdUtils;
    private final Random random = new Random();

    // 학생 가데이터 생성
    public void createStudents(List<Long> studentIds, List<String> studentPasswords) {
        int emailCounter = 1;
        Map<Integer, Set<Long>> gradeUsedSuffixMap = new HashMap<>();
        for (int grade = 1; grade <= 3; grade++) {
            int birthYear = 2010 - grade;
            String gradePrefix = String.valueOf(2026 - grade);
            gradeUsedSuffixMap.put(grade, new HashSet<>());

            for (int classId = 1; classId <= 7; classId++) {
                Set<String> usedNames = new HashSet<>();

                for (int number = 1; number <= 25; number++) {
                    String name = notProdUtils.generateRandomFullName(false);
                    usedNames.add(name);

                    LocalDate birthday = notProdUtils.generateRandomBirthday(birthYear, birthYear);
                    String password = notProdUtils.generatePasswordFromBirthday(birthday);

                    long studentSuffix;
                    do {
                        studentSuffix = 1000 + random.nextInt(90000); // 5자리
                    } while (gradeUsedSuffixMap.get(grade).contains(studentSuffix));
                    gradeUsedSuffixMap.get(grade).add(studentSuffix);
                    long accountId = Long.parseLong(gradePrefix + studentSuffix);

                    String email = "student" + (emailCounter++) + "@school.com";
                    String phone = notProdUtils.generateRandomPhone();

                    // 1~3학년 1반 1번 학생의 정보 저장
                    if (grade <= 3 && classId == 1 && number == 1) {
                        studentIds.add(accountId);
                        studentPasswords.add(password);
                    }
                    MemberForm student = MemberForm.builder()
                            .accountId(accountId)
                            .password(password)
                            .name(name)
                            .phone(phone)
                            .email(email)
                            .birthday(birthday)
                            .schoolName("송도고등학교")
                            .year(grade)
                            .classId(classId)
                            .number(number)
                            .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE)
                            .role(Member.MemberRole.ROLE_STUDENT)
                            .state(Member.State.NORMAL)
                            .build();
                    adminService.sudoSignup(student);
                }
            }
        }
        System.out.println("-- 총 525명의 학생 가데이터 생성 완료! --");
    }

    // 학부모 가데이터 생성
    public void createParents(List<Long> studentIds, List<String> studentPasswords, List<Long> parentIds) {
        for (int i = 0; i < 3; i++) {
            String name = notProdUtils.generateRandomFullName(true);
            int randomTwoDigits = random.nextInt(90) + 10;
            Long parentAccountId = Long.parseLong(studentIds.get(i) + String.valueOf(randomTwoDigits));
            log.info("parentAccountId", parentAccountId);
            ParentForm parentForm = ParentForm.builder()
                    .accountId(parentAccountId)
                    .password(studentPasswords.get(i))
                    .name(name)
                    .phone(notProdUtils.generateRandomPhone())
                    .email("parent" + (i + 1) + "@school.com")
                    .birthday(notProdUtils.generateRandomBirthday(1970, 1995))
                    .schoolName("송도고등학교")
                    .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE)
                    .build();
            memberService.signup(parentForm);
            parentIds.add(parentAccountId);
        }
        System.out.println("-- 학부모 3명 회원가입 완료! --");
    }

    // 선생님 가데이터 생성
    public void createTeachers(List<Long> teacherIds, List<String> teacherPasswords) {
        List<Member.Subject> subjectList = List.of(Member.Subject.values());
        int teacherEmailCount = 1;

        for (int year = 1; year <= 3; year++) {
            List<Integer> classIds = notProdUtils.getRandomUniqueNumbers(1, 7, 7);
            List<Member.Subject> shuffleSubjectList = new ArrayList<>(subjectList);
            Collections.shuffle(shuffleSubjectList);

            for (int i = 0; i < 17; i++) {
                String name = notProdUtils.generateRandomFullName(true);
                LocalDate birthday = notProdUtils.generateRandomBirthday(1970, 1995);
                String password = notProdUtils.generatePasswordFromBirthday(birthday);
                Long accountId = notProdUtils.generateAccountId();
                String phone = notProdUtils.generateRandomPhone();
                String email = "teacher" + (teacherEmailCount++) + "@school.com";

                MemberForm teacher = MemberForm.builder()
                        .accountId(accountId)
                        .password(password)
                        .name(name)
                        .phone(phone)
                        .email(email)
                        .birthday(birthday)
                        .schoolName("송도고등학교")
                        .year(year)
                        .subject(subjectList.get(i))
                        .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE)
                        .role(Member.MemberRole.ROLE_TEACHER)
                        .state(Member.State.NORMAL)
                        .build();
                if (i < 7) {
                    teacher.setClassId(classIds.get(i));
                }

                // 1~3학년 1반 선생님의 정보 저장
                if (year <= 3 && i < 7 && classIds.get(i) == 1) {
                    teacherIds.add(accountId);
                    teacherPasswords.add(password);
                }
                adminService.sudoSignup(teacher);
            }
        }
        System.out.println("-- 테스트용 교사 51명 생성 완료! --");
    }

    // 관리자 가데이터 생성
    public void createAdmin(List<Long> adminAccountIdHolder, List<String> adminPasswordHolder) {
        Long accountId = notProdUtils.generateRandom9DigitAccountId();
        String password = "iEdu77";
        String phone = notProdUtils.generateRandomPhone();

        MemberForm adminForm = MemberForm.builder()
                .accountId(accountId)
                .password(password)
                .name("김송도")
                .phone(phone)
                .email("admin1@school.com")
                .birthday(notProdUtils.generateRandomBirthday(1970, 1995))
                .schoolName("송도고등학교")
                .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE)
                .role(Member.MemberRole.ROLE_ADMIN)
                .state(Member.State.NORMAL)
                .build();
        adminService.sudoSignup(adminForm);
        adminAccountIdHolder.add(accountId);
        adminPasswordHolder.add(password);
        System.out.println("-- 관리자 1명 생성 완료! --");
    }
}
