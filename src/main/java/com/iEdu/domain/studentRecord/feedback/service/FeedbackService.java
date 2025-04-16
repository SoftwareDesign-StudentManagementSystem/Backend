package com.iEdu.domain.studentRecord.feedback.service;

import com.iEdu.domain.studentRecord.feedback.dto.req.FeedbackForm;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;

import java.util.List;

public interface FeedbackService {
    void create(FeedbackForm form);
    List<FeedbackDto> readByStudentId(Long studentId);
}
