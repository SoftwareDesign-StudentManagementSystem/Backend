package com.iEdu.domain.studentRecord.counsel.serviceImpl;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.domain.studentRecord.counsel.repository.CounselRepository;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CounselServiceImpl implements CounselService {

    private final CounselRepository counselRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Override
    @Transactional
    public void addCounsel(CounselRequest request) {
        // 상담 내용 유효성 검사
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ServiceException(ReturnCode.COUNSEL_CONTENT_REQUIRED);
        }

        // 상담 날짜 유효성 검사 (미래 제한)
        if (request.getDate() == null || request.getDate().isAfter(LocalDate.now())) {
            throw new ServiceException(ReturnCode.COUNSEL_DATE_INVALID);
        }

        // 학생/교사 존재 여부 확인
        Member student = memberRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));

        Member teacher = memberRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));

        // 저장
        Counsel counsel = new Counsel();
        counsel.setStudentId(student.getId());
        counsel.setTeacherId(teacher.getId());
        counsel.setDate(request.getDate());
        counsel.setContent(request.getContent().trim());
        counsel.setVisibleToStudent(request.isVisibleToStudent());
        counsel.setVisibleToParent(request.isVisibleToParent());
        counselRepository.save(counsel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CounselResponse> getCounselsByStudent(Long studentId) {
        // 존재 확인
        if (!memberRepository.existsById(studentId)) {
            throw new ServiceException(ReturnCode.USER_NOT_FOUND);
        }

        return counselRepository.findByStudentId(studentId).stream()
                .map(c -> new CounselResponse(
                        c.getDate(),
                        memberService.getMemberNameById(c.getTeacherId()),
                        c.getContent(),
                        c.getVisibleToStudent(),
                        c.getVisibleToParent()))
                .collect(Collectors.toList());
    }
}