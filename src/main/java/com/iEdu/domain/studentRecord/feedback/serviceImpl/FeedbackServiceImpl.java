package com.iEdu.domain.studentRecord.feedback.serviceImpl;

import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.domain.studentRecord.feedback.repository.FeedbackRepository;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;

    private String getTeacherName(Long teacherId) {
        return "박선생"; // TODO: Replace with actual teacher service
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
                        .teacher(getTeacherName(f.getTeacherId()))
                        .category(f.getCategory())
                        .content(f.getContent())
                        .visibleToStudent(f.getVisibleToStudent())
                        .visibleToParent(f.getVisibleToParent())
                        .build())
                .collect(Collectors.toList());
    }
}
