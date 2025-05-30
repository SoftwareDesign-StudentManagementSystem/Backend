package com.iEdu.domain.studentRecord.report.serviceImpl;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.studentRecord.attendance.dto.res.AttendanceDto;
import com.iEdu.domain.studentRecord.attendance.dto.res.PeriodAttendanceDto;
import com.iEdu.domain.studentRecord.attendance.entity.Attendance;
import com.iEdu.domain.studentRecord.attendance.repository.AttendanceRepository;
import com.iEdu.domain.studentRecord.attendance.service.AttendanceService;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselDto;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.domain.studentRecord.counsel.repository.CounselRepository;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.domain.studentRecord.feedback.repository.FeedbackRepository;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import com.iEdu.domain.studentRecord.grade.dto.res.GradeDto;
import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.repository.GradeRepository;
import com.iEdu.domain.studentRecord.grade.service.GradeService;
import com.iEdu.domain.studentRecord.report.dto.req.ReportForm;
import com.iEdu.domain.studentRecord.report.service.ReportGenerator;
import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyDto;
import com.iEdu.domain.studentRecord.specialty.entity.Specialty;
import com.iEdu.domain.studentRecord.specialty.repository.SpecialtyRepository;
import com.iEdu.domain.studentRecord.specialty.service.SpecialtyService;
import com.iEdu.global.common.enums.Semester;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component("EXCEL")
@RequiredArgsConstructor
public class ExcelReportGenerator implements ReportGenerator {
    private final FeedbackRepository feedbackRepository;
    private final FeedbackService feedbackService;
    private final CounselRepository counselRepository;
    private final CounselService counselService;
    private final SpecialtyRepository specialtyRepository;
    private final SpecialtyService specialtyService;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final GradeRepository gradeRepository;
    private final GradeService gradeService;

    @Override
    public byte[] generateSingleTypeReport(List<Member> students, Integer year, Semester semester, ReportForm form, String type) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(type); // 각 항목별 시트 이름: 성적, 피드백 등
            switch (type) {
                case "성적" -> writeGradeSection(sheet, students, year, semester);
                case "피드백" -> writeFeedbackSection(sheet, students, year, semester);
                case "상담" -> writeCounselSection(sheet, students, year, semester);
                case "특기사항" -> writeSpecialtySection(sheet, students, year, semester);
                default -> throw new IllegalArgumentException("알 수 없는 보고서 유형: " + type);
            }
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("엑셀 보고서 생성 중 오류 발생", e);
        }
    }

    @Override
    public byte[] generateReport(Member student, Integer year, Semester semester, ReportForm form) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (form.getAttendance()) {
                Sheet sheet = workbook.createSheet("출결");
                writeAttendanceSheet(sheet, student, year, semester);
            }
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("엑셀 생성 실패", e);
        }
    }

    @Override
    public String getFileExtension() {
        return "xlsx";
    }

    private void writeFeedbackSection(Sheet sheet, List<Member> students, Integer year, Semester semester) {
        Row header = sheet.createRow(0);
        String[] columns = {"날짜", "학생 이름", "학년", "반", "번호", "학기", "선생님 이름", "카테고리", "내용"};
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowIdx = 1;
        for (Member student : students) {
            List<Feedback> feedbacks = feedbackRepository.findByMemberIdAndYearAndSemester(student.getId(), year, semester);
            for (Feedback feedback : feedbacks) {
                FeedbackDto dto = feedbackService.convertToFeedbackDto(feedback);
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getDate().toString());
                row.createCell(1).setCellValue(student.getName());
                row.createCell(2).setCellValue(year);
                row.createCell(3).setCellValue(student.getClassId());
                row.createCell(4).setCellValue(student.getNumber());
                row.createCell(5).setCellValue(semester.toKoreanString());
                row.createCell(6).setCellValue(dto.getTeacherName());
                row.createCell(7).setCellValue(dto.getCategory().name());
                row.createCell(8).setCellValue(dto.getContent());
            }
        }
    }

    private void writeCounselSection(Sheet sheet, List<Member> students, Integer year, Semester semester) {
        Row header = sheet.createRow(0);
        String[] columns = {"날짜", "학생 이름", "학년", "반", "번호", "학기", "내용", "다음 상담예정일"};
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowIdx = 1;
        for (Member student : students) {
            List<Counsel> counsels = counselRepository.findByMemberIdAndYearAndSemester(student.getId(), year, semester);
            for (Counsel counsel : counsels) {
                CounselDto dto = counselService.convertToCounselDto(counsel);
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getDate().toString());
                row.createCell(1).setCellValue(student.getName());
                row.createCell(2).setCellValue(year);
                row.createCell(3).setCellValue(student.getClassId());
                row.createCell(4).setCellValue(student.getNumber());
                row.createCell(5).setCellValue(semester.toKoreanString());
                row.createCell(6).setCellValue(dto.getContent());
                row.createCell(7).setCellValue(dto.getNextCounselDate() != null ? dto.getNextCounselDate().toString() : "");
            }
        }
    }

    private void writeSpecialtySection(Sheet sheet, List<Member> students, Integer year, Semester semester) {
        Row header = sheet.createRow(0);
        String[] columns = {"날짜", "학생 이름", "학년", "반", "번호", "학기", "내용"};
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowIdx = 1;
        for (Member student : students) {
            List<Specialty> specialties = specialtyRepository.findByMemberIdAndYearAndSemester(student.getId(), year, semester);
            for (Specialty specialty : specialties) {
                SpecialtyDto dto = specialtyService.convertToSpecialtyDto(specialty);
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getDate().toString());
                row.createCell(2).setCellValue(year);
                row.createCell(3).setCellValue(student.getClassId());
                row.createCell(4).setCellValue(student.getNumber());
                row.createCell(5).setCellValue(semester.toKoreanString());
                row.createCell(6).setCellValue(dto.getContent());            }
        }
    }

    private void writeAttendanceSheet(Sheet sheet, Member student, Integer year, Semester semester) {
        Row infoRow = sheet.createRow(0);
        infoRow.createCell(0).setCellValue("학생 이름");
        infoRow.createCell(1).setCellValue("학년");
        infoRow.createCell(2).setCellValue("반");
        infoRow.createCell(3).setCellValue("번호");
        infoRow.createCell(4).setCellValue("학기");

        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue(student.getName());
        dataRow.createCell(1).setCellValue(year);
        dataRow.createCell(2).setCellValue(student.getClassId());
        dataRow.createCell(3).setCellValue(student.getNumber());
        dataRow.createCell(4).setCellValue(semester.toKoreanString());

        Row header = sheet.createRow(2);
        header.createCell(0).setCellValue("날짜");
        for (int i = 1; i <= 8; i++) {
            header.createCell(i).setCellValue(i + "교시");
        }

        List<Attendance> attendances = attendanceRepository.findByMemberIdAndYearAndSemesterOrderByDateAsc(student.getId(), year, semester);
        int rowIdx = 3;
        for (Attendance attendance : attendances) {
            AttendanceDto dto = attendanceService.convertToAttendanceDto(attendance);
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(dto.getDate().toString());

            for (PeriodAttendanceDto period : dto.getPeriodAttendanceDtos()) {
                int col = period.getPeriod().getValue(); // assume Period 1~8
                String symbol = switch (period.getState()) {
                    case 출석 -> "O";
                    case 결석 -> "❤";
                    case 지각 -> "X";
                    case 조퇴 -> "◎";
                };
                row.createCell(col).setCellValue(symbol);
            }
        }
    }

    private void writeGradeSection(Sheet sheet, List<Member> students, Integer year, Semester semester) {
        Row header = sheet.createRow(0);
        String[] columns = {
                "학생 이름", "학년", "반", "번호", "학기", "국어", "수학", "영어", "사회", "한국사", "윤리", "경제",
                "물리", "화학", "생명과학", "지구과학", "음악", "미술", "체육", "기술가정", "컴퓨터", "제2외국어", "학년 석차"
        };
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowIdx = 1;
        for (Member student : students) {
            Optional<Grade> optionalGrade = gradeRepository.findByMemberIdAndYearAndSemester(student.getId(), year, semester);
            if (optionalGrade.isPresent()) {
                GradeDto dto = gradeService.convertToGradeDto(optionalGrade.get(), student.getAccountId());
                Row row = sheet.createRow(rowIdx++);
                int cellIdx = 0;
                row.createCell(cellIdx++).setCellValue(student.getName());
                row.createCell(cellIdx++).setCellValue(year);
                row.createCell(cellIdx++).setCellValue(student.getClassId());
                row.createCell(cellIdx++).setCellValue(student.getNumber());
                row.createCell(cellIdx++).setCellValue(semester.toKoreanString());
                row.createCell(cellIdx++).setCellValue(dto.get국어().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get수학().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get영어().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get사회().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get한국사().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get윤리().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get경제().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get물리().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get화학().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get생명과학().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get지구과학().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get음악().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get미술().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get체육().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get기술가정().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get컴퓨터().getScore());
                row.createCell(cellIdx++).setCellValue(dto.get제2외국어().getScore());
                row.createCell(cellIdx++).setCellValue(dto.getGradeRank());
            }
        }
    }
}
