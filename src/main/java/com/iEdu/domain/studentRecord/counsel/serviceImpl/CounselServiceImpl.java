package com.iEdu.domain.studentRecord.counsel.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CounselServiceImpl implements CounselService {

    private final CounselRepository counselRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Override
    @Transactional
    // 선생님 전용
    public void addCounsel(CounselRequest request, LoginUserDto loginUser){
        // 보안 검사
        if (loginUser.getRole() == Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }

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

        Member teacher = memberRepository.findById(loginUser.getId())
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



    public Page<CounselResponse> getCounsels(Long studentId, Pageable pageable,
                                             LoginUserDto loginUser,
                                             LocalDate startDate, LocalDate endDate,
                                             String teacherName) {
        // 보안
        if (!memberRepository.existsById(studentId)) {
            throw new ServiceException(ReturnCode.USER_NOT_FOUND);
        }

        // 기본값 지정
        if (startDate == null) startDate = LocalDate.of(1900, 1, 1);
        if (endDate == null) endDate = LocalDate.of(2100, 1, 1);

        Page<Counsel> page = counselRepository.findByStudentIdAndDateBetween(studentId, startDate, endDate, pageable);

        // 2차 필터: 선생 이름
        Page<CounselResponse> result = page.map(counsel -> new CounselResponse(
                counsel.getDate(),
                memberService.getMemberNameById(counsel.getTeacherId()),
                counsel.getContent(),
                counsel.getVisibleToStudent(),
                counsel.getVisibleToParent()
        ));

        // Optional 필터링: teacherName이 들어온 경우만 필터링
        if (teacherName.isBlank()) {
            result = new PageImpl<>(
                    result.stream()
                            .filter(c -> c.getTeacher().contains(teacherName))
                            .toList(),
                    pageable,
                    result.getTotalElements()
            );
        }
        return result;
    }
}