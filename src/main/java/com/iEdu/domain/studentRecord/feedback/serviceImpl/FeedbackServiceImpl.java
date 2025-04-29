package com.iEdu.domain.studentRecord.feedback.serviceImpl;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackCategory;
import com.iEdu.domain.studentRecord.feedback.repository.FeedbackRepository;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;

    private String getTeacherName(Long teacherId) {
        Member teacher = memberRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        return teacher.getName();
    }

    @Override
    public void create(FeedbackForm form) {
        Feedback feedback = Feedback.builder()
                .studentId(form.getStudentId())
                .teacherId(form.getTeacherId())
                .category(form.getCategory())
                .content(form.getContent())
                .visibleToStudent(form.getVisibleToStudent())
                .visibleToParent(form.getVisibleToParent())
                .build();
        feedbackRepository.save(feedback);
    }

    @Override
    public List<FeedbackDto> readByStudentId(Long studentId) {
        return feedbackRepository.findByStudentId(studentId).stream()
                .map(f -> FeedbackDto.builder()
                        .studentId(f.getStudentId())
                        .teacher(getTeacherName(f.getTeacherId()))
                        .category(f.getCategory())
                        .content(f.getContent())
                        .recordedDate(f.getRecordedDate())
                        .visibleToStudent(f.getVisibleToStudent())
                        .visibleToParent(f.getVisibleToParent())
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    public List<FeedbackDto> searchByFilters(Long studentId,
                                             Long teacherId,
                                             FeedbackCategory category,
                                             LocalDate startDate,
                                             LocalDate endDate) {

        return feedbackRepository.searchByFilters(studentId, startDate, endDate, teacherId, category)
                .stream()
                .map(f -> FeedbackDto.builder()
                        .teacher(getTeacherName(f.getTeacherId()))
                        .category(f.getCategory())
                        .content(f.getContent())
                        .recordedDate(f.getRecordedDate())
                        .visibleToStudent(f.getVisibleToStudent())
                        .visibleToParent(f.getVisibleToParent())
                        .build())
                .collect(Collectors.toList());
    }
}
