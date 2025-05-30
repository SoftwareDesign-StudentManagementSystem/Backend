package com.iEdu.domain.studentRecord.report.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.report.dto.req.ReportForm;
import com.iEdu.domain.studentRecord.report.dto.res.ReportDto;
import com.iEdu.domain.studentRecord.report.service.ReportService;
import com.iEdu.global.common.enums.ReportFormat;
import com.iEdu.domain.studentRecord.report.service.ReportGenerator;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import com.iEdu.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.iEdu.global.common.utils.RoleValidator.validateTeacherRole;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final MemberRepository memberRepository;
    private final S3Service s3Service;
    private final Map<String, ReportGenerator> reportGenerators;

    // 학생 보고서 생성 및 다운로드 [선생님 권한]
    @Override
    @Transactional
    public ReportDto generateReport(ReportForm form, LoginUserDto loginUser) {
        validateTeacherRole(loginUser);
        List<Member> students = form.getStudentIdList().stream()
                .map(id -> memberRepository.findById(id)
                        .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND)))
                .collect(Collectors.toList());
        Map<String, String> reportUrls = new LinkedHashMap<>(); // 유지 순서 보장

        List<Semester> targetSemesters = form.getSemester() == Semester.ALL
                ? List.of(Semester.FIRST_SEMESTER, Semester.SECOND_SEMESTER)
                : List.of(form.getSemester());
        for (Semester semester : targetSemesters) {
            List<ReportFormat> formats = form.getReportFormat() == ReportFormat.ALL
                    ? List.of(ReportFormat.PDF, ReportFormat.EXCEL)
                    : List.of(form.getReportFormat());

            for (ReportFormat format : formats) {
                ReportGenerator generator = reportGenerators.get(format.name());

                if (form.getAttendance()) {
                    // 출결 보고서 → 학생별 개별 생성
                    for (Member student : students) {
                        uploadSingleReport(
                                generator,
                                student,
                                form.getYear(),
                                semester,
                                form,
                                "출결",
                                semester.name().toLowerCase(),
                                format,
                                generator.getFileExtension(),
                                reportUrls
                        );
                    }
                }
                // 성적/상담/피드백/특기사항 → 통합 생성
                for (String type : List.of("성적", "상담", "피드백", "특기사항")) {
                    if (isTypeEnabled(form, type)) {
                        ReportForm singleForm = form.copyWithOnly(type);
                        byte[] file = generator.generateSingleTypeReport(students, form.getYear(), semester, singleForm, type);
                        String fileKey = String.format("reports/%s_%s_%s.%s", type, semester.name().toLowerCase(), UUID.randomUUID(), generator.getFileExtension());
                        s3Service.uploadFile(file, fileKey, getContentType(generator.getFileExtension()), fileKey);
                        putPresignedUrl(
                                reportUrls,
                                type,
                                semester.name().toLowerCase(),
                                format,
                                generator.getFileExtension(),
                                fileKey,
                                null, // student = null → 전체 보고서 의미
                                form.getYear()
                        );
                    }
                }
            }
        }
        return new ReportDto(reportUrls); // 변경된 생성자 사용
    }

    // ----------------- 헬퍼 메서드 -----------------

    private String getContentType(String ext) {
        return ext.equals("pdf") ? "application/pdf" :
                ext.equals("xlsx") ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "application/octet-stream";
    }

    private String buildDownloadFileName(String prefix, Semester semester, String extension, int actualYear) {
        return String.format("%d년도_%s_%s.%s",
                actualYear,
                semester.toKoreanString(),
                prefix,
                extension);
    }

    private void uploadSingleReport(
            ReportGenerator generator,
            Member student,
            int year,
            Semester semester,
            ReportForm form,
            String type,
            String suffix,
            ReportFormat format,
            String extension,
            Map<String, String> reportUrls
    ) {
        // form의 카피를 만들고 해당 type만 true로 설정
        ReportForm singleForm = form.copyWithOnly(type);
        byte[] file = generator.generateReport(student, year, semester, singleForm);
        String fileKey = String.format("reports/%s_%s.%s", student.getId(), UUID.randomUUID(), extension);
        s3Service.uploadFile(file, fileKey, getContentType(extension), fileKey);
        putPresignedUrl(reportUrls, type, suffix, format, extension, fileKey, student, year);
    }

    private void putPresignedUrl(
            Map<String, String> map,
            String type,
            String suffix,
            ReportFormat format,
            String extension,
            String fileKey,
            Member student,  // null일 수 있음
            int formYear
    ) {
        Semester semester = Semester.valueOf(suffix.toUpperCase());

        int actualYear = student == null
                ? LocalDate.now().getYear()  // 전체 보고서는 현재 연도 기준으로 처리하거나 formYear로 처리
                : Integer.parseInt(student.getAccountId().toString().substring(0, 4)) + (formYear - 1);

        String downloadFileName = student == null
                ? buildDownloadFileName(type + "_전체", semester, extension, actualYear)
                : (type.equals("출결")
                ? String.format("%d년도_%s_%s_%s_%s.%s",
                actualYear, semester.toKoreanString(), student.getAccountId(), student.getName(), type, extension)
                : buildDownloadFileName(type, semester, extension, actualYear));

        String mapKey = student == null
                ? String.format("%d년도_%s_전체_%s_%s",
                actualYear, semester.toKoreanString(), type, format.name().toLowerCase())
                : (type.equals("출결")
                ? String.format("%d년도_%s_%s_%s_%s_%s",
                actualYear, semester.toKoreanString(), student.getAccountId(), student.getName(), type, format.name().toLowerCase())
                : String.format("%d년도_%s_%s_%s",
                actualYear, semester.toKoreanString(), type, format.name().toLowerCase()));
        String presignedUrl = s3Service.generatePresignedUrl(fileKey, downloadFileName);
        map.put(mapKey, presignedUrl);
    }

    private boolean isTypeEnabled(ReportForm form, String type) {
        return switch (type) {
            case "성적" -> form.getGrade();
            case "상담" -> form.getCounsel();
            case "피드백" -> form.getFeedback();
            case "특기사항" -> form.getSpecialty();
            default -> false;
        };
    }
}
