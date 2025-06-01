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
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.iEdu.domain.studentRecord.attendance.entity.PeriodAttendance.State.*;

@Component("PDF")
@RequiredArgsConstructor
public class PdfReportGenerator implements ReportGenerator {
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
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont fontBold = createFont("fonts/NotoSansKR-Bold.ttf");
            PdfFont fontRegular = createFont("fonts/NotoSansKR-Regular.ttf");
            document.setFont(fontRegular);
            switch (type) {
                case "성적" -> addGradeSection(document, students, year, semester, fontBold, fontRegular);
                case "피드백" -> addFeedbackSection(document, students, year, semester, fontBold, fontRegular);
                case "상담" -> addCounselSection(document, students, year, semester, fontBold, fontRegular);
                case "특기사항" -> addSpecialtySection(document, students, year, semester, fontBold, fontRegular);
                default -> throw new IllegalArgumentException("Unknown report type: " + type);
            }
            document.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF 생성 중 오류 발생", e);
        }
    }

    @Override
    public byte[] generateReport(Member student, Integer year, Semester semester, ReportForm form) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // 폰트 두 개 로드
            PdfFont fontBold = createFont("fonts/NotoSansKR-Bold.ttf");
            PdfFont fontRegular = createFont("fonts/NotoSansKR-Regular.ttf");

            // 문서 기본 폰트는 Regular
            document.setFont(fontRegular);
            if (form.getAttendance()) {
                addAttendanceSection(document, student, year, semester, fontBold, fontRegular);
            }
            document.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF 생성 중 오류 발생", e);
        }
    }

    @Override
    public String getFileExtension() {
        return "pdf";
    }

    private void addFeedbackSection(Document doc, List<Member> students, Integer year, Semester semester,
                                    PdfFont fontBold, PdfFont fontRegular) {
        doc.add(new Paragraph("피드백").setFont(fontBold).setFontSize(7));
        Table table = new Table(9);
        addTableHeaders(table, fontBold, "날짜", "학생 이름", "학년", "반", "번호", "학기", "선생님 이름", "카테고리", "내용");

        for (Member student : students) {
            List<Feedback> feedbacks = feedbackRepository.findByMemberIdAndYearAndSemester(student.getId(), year, semester);
            for (Feedback feedback : feedbacks) {
                FeedbackDto dto = feedbackService.convertToFeedbackDto(feedback);
                table.addCell(new Paragraph(dto.getDate().toString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(student.getName()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(year.toString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(String.valueOf(student.getClassId())).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(String.valueOf(student.getNumber())).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(semester.toKoreanString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(dto.getTeacherName()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(dto.getCategory().name()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(dto.getContent()).setFont(fontRegular).setFontSize(7));
            }
        }
        doc.add(table).add(new Paragraph("\n"));
    }

    private void addCounselSection(Document doc, List<Member> students, Integer year, Semester semester,
                                   PdfFont fontBold, PdfFont fontRegular) {
        doc.add(new Paragraph("상담").setFont(fontBold).setFontSize(7));
        Table table = new Table(9);
        addTableHeaders(table, fontBold, "날짜", "학생 이름", "학년", "반", "번호", "학기", "선생님 이름", "내용", "다음 상담예정일");

        for (Member student : students) {
            List<Counsel> counsels = counselRepository.findByMemberIdAndYearAndSemester(student.getId(), year, semester);
            for (Counsel counsel : counsels) {
                CounselDto dto = counselService.convertToCounselDto(counsel);
                table.addCell(new Paragraph(dto.getDate().toString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(student.getName()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(year.toString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(String.valueOf(student.getClassId())).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(String.valueOf(student.getNumber())).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(semester.toKoreanString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(dto.getTeacherName()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(dto.getContent()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(dto.getNextCounselDate() != null ? dto.getNextCounselDate().toString() : "").setFont(fontRegular).setFontSize(7));
            }
        }
        doc.add(table).add(new Paragraph("\n"));
    }

    private void addSpecialtySection(Document doc, List<Member> students, Integer year, Semester semester,
                                     PdfFont fontBold, PdfFont fontRegular) {
        doc.add(new Paragraph("특기사항").setFont(fontBold).setFontSize(7));
        Table table = new Table(8);
        addTableHeaders(table, fontBold, "날짜", "학생 이름", "학년", "반", "번호", "학기", "선생님 이름", "내용");

        for (Member student : students) {
            List<Specialty> specialties = specialtyRepository.findByMemberIdAndYearAndSemester(student.getId(), year, semester);
            for (Specialty specialty : specialties) {
                SpecialtyDto dto = specialtyService.convertToSpecialtyDto(specialty);
                table.addCell(new Paragraph(dto.getDate().toString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(student.getName()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(year.toString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(String.valueOf(student.getClassId())).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(String.valueOf(student.getNumber())).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(semester.toKoreanString()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(dto.getTeacherName()).setFont(fontRegular).setFontSize(7));
                table.addCell(new Paragraph(dto.getContent()).setFont(fontRegular).setFontSize(7));
            }
        }
        doc.add(table).add(new Paragraph("\n"));
    }

    private void addAttendanceSection(Document doc, Member student, Integer year, Semester semester,
                                      PdfFont fontBold, PdfFont fontRegular) {
        doc.add(new Paragraph("출결").setFont(fontBold).setFontSize(7));
        doc.add(new Paragraph("학생 이름: " + student.getName() + "   " + year + "학년   " + student.getClassId() + "반   "
                + student.getNumber() + "번   " + semester.toKoreanString()).setFont(fontRegular).setFontSize(7));

        Table table = new Table(9);
        addTableHeaders(table, fontBold, "날짜", "1교시", "2교시", "3교시", "4교시", "5교시", "6교시", "7교시", "8교시");

        List<Attendance> attendances = attendanceRepository.findByMemberIdAndYearAndSemesterOrderByDateAsc(student.getId(), year, semester);
        for (Attendance attendance : attendances) {
            AttendanceDto dto = attendanceService.convertToAttendanceDto(attendance);
            Map<Integer, String> periodMap = dto.getPeriodAttendanceDtos().stream()
                    .collect(Collectors.toMap(
                            p -> p.getPeriod().getValue(),
                            p -> switch (p.getState()) {
                                case 출석 -> "O";
                                case 결석 -> "V";
                                case 지각 -> "X";
                                case 조퇴 -> "◎";
                                default -> "";
                            }
                    ));

            table.addCell(new Paragraph(dto.getDate().toString()).setFont(fontRegular).setFontSize(7));
            for (int i = 1; i <= 8; i++) {
                table.addCell(new Paragraph(periodMap.getOrDefault(i, "")).setFont(fontRegular).setFontSize(7));
            }
        }

        doc.add(table).add(new Paragraph("\n"));
    }

    private void addGradeSection(Document doc, List<Member> students, Integer year, Semester semester,
                                 PdfFont fontBold, PdfFont fontRegular) {
        doc.add(new Paragraph("성적").setFont(fontBold).setFontSize(7));
        Table table = new Table(23);
        table.setFontSize(7);
        String[] headers = {
                "학생 이름", "학년", "반", "번호", "학기", "국어", "수학", "영어", "사회", "한국사", "윤리", "경제",
                "물리", "화학", "생명과학", "지구과학", "음악", "미술", "체육", "기술가정", "컴퓨터", "제2외국어", "학년 석차"
        };
        addTableHeaders(table, fontBold, headers);

        for (Member student : students) {
            Optional<Grade> optionalGrade = gradeRepository.findByMemberIdAndYearAndSemester(student.getId(), year, semester);
            if (optionalGrade.isPresent()) {
                GradeDto dto = gradeService.convertToGradeDto(optionalGrade.get(), student.getAccountId());

                table.addCell(new Cell().add(new Paragraph(student.getName()).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(year.toString()).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(student.getClassId())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(student.getNumber())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(semester.toKoreanString()).setFont(fontRegular).setFontSize(7)));

                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get국어().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get수학().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get영어().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get사회().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get한국사().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get윤리().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get경제().getScore())).setFont(fontRegular).setFontSize(7)));

                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get물리().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get화학().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get생명과학().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get지구과학().getScore())).setFont(fontRegular).setFontSize(7)));

                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get음악().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get미술().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get체육().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get기술가정().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get컴퓨터().getScore())).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.get제2외국어().getScore())).setFont(fontRegular).setFontSize(7)));

                table.addCell(new Cell().add(new Paragraph(String.valueOf(dto.getGradeRank())).setFont(fontRegular).setFontSize(7)));
            } else {
                // 성적 데이터 없으면 빈 칸 혹은 적절한 처리
                table.addCell(new Cell().add(new Paragraph(student.getName()).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(year.toString()).setFont(fontRegular).setFontSize(7)));
                table.addCell(new Cell().add(new Paragraph(semester.toKoreanString()).setFont(fontRegular).setFontSize(7)));
                for (int i = 0; i < 18; i++) {
                    table.addCell(new Cell().add(new Paragraph("-").setFont(fontRegular).setFontSize(7)));
                }
            }
        }
        doc.add(table);
    }

    // ----------------- 헬퍼 메서드 -----------------

    private PdfFont createFont(String resourcePath) throws IOException {
        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (fontStream == null) {
                throw new RuntimeException("폰트 파일을 찾을 수 없습니다: " + resourcePath);
            }
            byte[] fontBytes = fontStream.readAllBytes();
            FontProgram fontProgram = FontProgramFactory.createFont(fontBytes);
            return PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H);
        }
    }

    private void addTableHeaders(Table table, PdfFont fontBold, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setFont(fontBold).setFontSize(7));
            table.addHeaderCell(cell);
        }
    }
}
