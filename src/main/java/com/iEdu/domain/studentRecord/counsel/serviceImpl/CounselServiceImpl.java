package com.iEdu.domain.studentRecord.counsel.serviceImpl;

import com.iEdu.domain.account.member.service.MemberService;
import com.iEdu.domain.studentRecord.counsel.dto.req.CounselRequest;
import com.iEdu.domain.studentRecord.counsel.dto.res.CounselResponse;
import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.domain.studentRecord.counsel.repository.CounselRepository;
import com.iEdu.domain.studentRecord.counsel.service.CounselService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CounselServiceImpl implements CounselService {

    private final CounselRepository counselRepository;
    private final MemberService memberService; // 교사 이름 조회용

    @Override
    public void addCounsel(CounselRequest request) {
        Counsel counsel = new Counsel();
        counsel.setStudentId(request.getStudentId());
        counsel.setTeacherId(request.getTeacherId());
        counsel.setDate(request.getDate());
        counsel.setContent(request.getContent());
        counsel.setVisibleToStudent(request.isVisibleToStudent());
        counsel.setVisibleToParent(request.isVisibleToParent());
        counselRepository.save(counsel);
    }

    @Override
    public List<CounselResponse> getCounselsByStudent(Long studentId) {
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
