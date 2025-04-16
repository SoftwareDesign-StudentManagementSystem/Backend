package com.iEdu.global.initData;

import com.iEdu.domain.account.admin.service.AdminService;
import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.req.MemberForm;
import com.iEdu.domain.account.member.dto.req.ParentForm;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.studentRecord.grade.dto.req.GradeForm;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.time.YearMonth;

import static com.iEdu.domain.account.member.entity.Member.Subject.*;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class NotProd {
    private final AdminService adminService;
    private final GradeService gradeService;
    private final MemberRepository memberRepository;
    private final Random random = new Random();
    private static final String[] SURNAMES = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"};
    private static final String[] FIRST_NAMES = {
            "민준", "서준", "예준", "도윤", "시우", "하준", "주원", "지후", "지훈", "준서",
            "서연", "서윤", "하은", "지우", "지유", "하린", "수아", "서현", "예은", "채원"
    };

    @Bean
    public ApplicationRunner applicationRunner(MemberService memberService){
        return args -> {
            // 학생 가데이터 생성 로직
            int emailCounter = 1;

            Long firstStudentAccountId = null;
            String firstStudentPassword = null;
            Map<Integer, Set<Long>> gradeUsedSuffixMap = new HashMap<>();

            for (int grade = 1; grade <= 3; grade++) {
                int birthYear = 2010 - grade; // 1학년: 2009, 2학년: 2008, 3학년: 2007
                String gradePrefix = String.valueOf(2026 - grade); // 1학년: 2025, 2학년: 2024, 3학년: 2023
                gradeUsedSuffixMap.put(grade, new HashSet<>()); // grade별 초기화

                for (int classId = 1; classId <= 7; classId++) {
                    Set<String> usedFullNames = new HashSet<>();

                    for (int number = 1; number <= 25; number++) {
                        String fullName;
                        do {
                            String surname = SURNAMES[random.nextInt(SURNAMES.length)];
                            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                            fullName = surname + firstName;
                        } while (usedFullNames.contains(fullName));
                        usedFullNames.add(fullName);

                        int month = random.nextInt(12) + 1;
                        int day = random.nextInt(28) + 1; // 단순화

                        LocalDate birthday = LocalDate.of(birthYear, month, day);
                        String password = String.format("%02d%02d%02d", birthYear % 100, month, day);

                        long randomSuffix;
                        do {
                            randomSuffix = 10000 + random.nextInt(90000); // 5자리
                        } while (gradeUsedSuffixMap.get(grade).contains(randomSuffix));
                        gradeUsedSuffixMap.get(grade).add(randomSuffix);
                        Long accountId = Long.parseLong(gradePrefix + randomSuffix);

                        String phone = String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000));
                        String email = "student" + (emailCounter++) + "@shcool.com";

                        // 1학년 1반 1번 학생의 정보 저장
                        if (grade == 1 && classId == 1 && number == 1) {
                            firstStudentAccountId = accountId;
                            firstStudentPassword = password;
                        }

                        MemberForm student = MemberForm.builder()
                                .accountId(accountId)
                                .password(password)
                                .name(fullName)
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
            System.out.println("-- 테스트용 학생 525명 생성 완료! --");

            // 학부모 가데이터 생성 로직
            int randomTwoDigits = random.nextInt(90) + 10; // 10~99
            Long parentAccountId = Long.parseLong(firstStudentAccountId + String.valueOf(randomTwoDigits));

            ParentForm parentForm = ParentForm.builder()
                    .accountId(parentAccountId)
                    .password(firstStudentPassword)
                    .name("이선영")
                    .phone(String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000)))
                    .email("parent@school.com")
                    .birthday(generateRandomBirthday(1970, 1995))
                    .schoolName("송도고등학교")
                    .gender(Member.Gender.FEMALE)
                    .build();
            memberService.signup(parentForm);
            System.out.println("-- 학부모 1명 회원가입 완료! --");

            // 선생님 가데이터 생성 로직
            List<Member.Subject> subjectList = List.of(
                    KOREAN_LANGUAGE, MATHEMATICS, ENGLISH,
                    Member.Subject.SOCIAL_STUDIES, Member.Subject.HISTORY, Member.Subject.ETHICS, Member.Subject.ECONOMICS,
                    Member.Subject.PHYSICS, Member.Subject.CHEMISTRY, Member.Subject.BIOLOGY, Member.Subject.EARTH_SCIENCE,
                    Member.Subject.MUSIC, Member.Subject.ART, Member.Subject.PHYSICAL_EDUCATION,
                    Member.Subject.TECHNOLOGY_AND_HOME_ECONOMICS, Member.Subject.COMPUTER_SCIENCE,
                    Member.Subject.SECOND_FOREIGN_LANGUAGE
            );

            String[] oldFirstNames = {
                    "영희", "철수", "말자", "순자", "명자", "춘자", "옥자", "영자", "숙자", "정자",
                    "갑순", "기순", "옥순", "순희", "순복", "용자", "형철", "병수", "춘호", "길동"
            };

            int teacherEmailCount = 1;

            for (int year = 1; year <= 3; year++) {
                List<Integer> classIds = getRandomUniqueNumbers(1, 7, 7); // 7개 반 배정용
                List<Member.Subject> shuffleSubjectList = new ArrayList<>(subjectList);
                Collections.shuffle(shuffleSubjectList); // 과목 셔플

                for (int i = 0; i < 17; i++) {
                    String surname = SURNAMES[random.nextInt(SURNAMES.length)];
                    String firstName = oldFirstNames[random.nextInt(oldFirstNames.length)];
                    String fullName = surname + firstName;

                    int birthYear = 1970 + random.nextInt(1995 - 1970 + 1);
                    int month = random.nextInt(12) + 1;
                    int day = random.nextInt(28) + 1;
                    LocalDate birthday = LocalDate.of(birthYear, month, day);
                    String password = String.format("%02d%02d%02d", birthYear % 100, month, day);

                    int accountPrefix = 2000 + random.nextInt(25); // 2000~2024
                    long accountSuffix = 10000 + random.nextInt(90000);
                    Long accountId = Long.parseLong(accountPrefix + String.valueOf(accountSuffix));

                    String phone = String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000));
                    String email = "teacher" + (teacherEmailCount++) + "@school.com";

                    MemberForm teacher = MemberForm.builder()
                            .accountId(accountId)
                            .password(password)
                            .name(fullName)
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
                        teacher.setClassId(classIds.get(i)); // 7명만 반 담당 배정
                    }
                    adminService.sudoSignup(teacher);
                }
            }
            System.out.println("-- 테스트용 교사 51명 생성 완료! --");

            // 관리자 가데이터 생성 로직
            String phone = String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000));

            MemberForm adminForm = MemberForm.builder()
                    .accountId(Long.parseLong(String.format("%09d", ThreadLocalRandom.current().nextLong(1_000_000_000L))))
                    .password("iEdu77")
                    .name("김송도")
                    .phone(phone) // 아래 함수 참고
                    .email("admin1@school.com")
                    .birthday(generateRandomBirthday(1970, 1995)) // 아래 함수 참고
                    .schoolName("송도고등학교")
                    .gender(random.nextBoolean() ? Member.Gender.MALE : Member.Gender.FEMALE) // 아래 함수 참고
                    .role(Member.MemberRole.ROLE_ADMIN)
                    .state(Member.State.NORMAL)
                    .build();
            adminService.sudoSignup(adminForm);
            System.out.println("-- 관리자 1명 생성 완료! --");

            // 성적 가데이터 생성 로직
            List<Member> teacherList = memberRepository.findAll().stream()
                    .filter(m -> m.getRole() == Member.MemberRole.ROLE_TEACHER)
                    .toList();
            Random random = new Random();
            for (int studentGrade = 1; studentGrade <= 3; studentGrade++) {
                int startId = (studentGrade - 1) * 175 + 1;
                int endId = studentGrade * 175;
                for (int studentId = startId; studentId <= endId; studentId++) {
                    for (int targetGrade = 1; targetGrade <= studentGrade; targetGrade++) {
                        // 해당 학년 담당 교사들 추출 (17명)
                        int finalGrade = targetGrade;
                        List<Member> teachersForGrade = teacherList.stream()
                                .filter(t -> t.getYear() == finalGrade)
                                .collect(Collectors.toList());
                        for (int subjectIdx = 0; subjectIdx < subjectList.size(); subjectIdx++) {
                            Member.Subject subject = subjectList.get(subjectIdx);
                            Member teacher = teachersForGrade.get(subjectIdx);
                            LoginUserDto loginUser = LoginUserDto.ConvertToLoginUserDto(teacher);
                            double rawScore = 60 + random.nextDouble() * 40;
                            double roundedScore = Math.round(rawScore * 10) / 10.0;
                            for (Grade.Semester semester : Grade.Semester.values()) {
                                GradeForm gradeForm = GradeForm.builder()
                                        .year(targetGrade)
                                        .semester(semester)
                                        .score(roundedScore) // 60~100
                                        .build();
                                try {
                                    gradeService.createGrade((long) studentId, gradeForm, loginUser);
                                } catch (Exception e) {
                                    System.out.println("⚠️ 성적 생성 실패: studentId=" + studentId +
                                            ", subject=" + subject + ", year=" + targetGrade +
                                            ", semester=" + semester + ", reason=" + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("-- 총 2,100개의 성적 데이터 생성 완료! --");
            System.out.println("-- 1학년 1반 1번 학생 --");
            System.out.println("accountId: " + firstStudentAccountId);
            System.out.println("parentId: " + parentAccountId);
            System.out.println("password: " + firstStudentPassword);
        };
    }

    private List<Integer> getRandomUniqueNumbers(int start, int end, int count) {
        List<Integer> numbers = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
        Collections.shuffle(numbers);
        return numbers.subList(0, count);
    }

    private LocalDate generateRandomBirthday(int startYear, int endYear) {
        int year = ThreadLocalRandom.current().nextInt(startYear, endYear + 1);
        int month = ThreadLocalRandom.current().nextInt(1, 13);
        int day = ThreadLocalRandom.current().nextInt(1, YearMonth.of(year, month).lengthOfMonth() + 1);
        return LocalDate.of(year, month, day);
    }
}
