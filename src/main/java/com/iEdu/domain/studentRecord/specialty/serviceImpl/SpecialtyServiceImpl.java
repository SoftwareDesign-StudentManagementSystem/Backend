package com.iEdu.domain.studentRecord.specialty.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.specialty.dto.req.SpecialtyForm;
import com.iEdu.domain.studentRecord.specialty.dto.res.SpecialtyDto;
import com.iEdu.domain.studentRecord.specialty.entity.Specialty;
import com.iEdu.domain.studentRecord.specialty.repository.SpecialtyRepository;
import com.iEdu.domain.studentRecord.specialty.service.SpecialtyService;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final MemberRepository memberRepository;

    /**
     * 학생의 특기사항 전체 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<SpecialtyDto> getAllSpecialties(Long studentId, LoginUserDto loginUser) {
        Member.MemberRole role = loginUser.getRole();
        if (role == Member.MemberRole.ROLE_TEACHER) {
            return convertToDtoList(
                    specialtyRepository.findAllByMemberIdOrderByIdDesc(studentId)
            );
        }
        if (role == Member.MemberRole.ROLE_PARENT) {
            boolean isFollowed = loginUser.getFollowList().stream()
                    .anyMatch(f -> f.getFollow().getId().equals(studentId));
            if (!isFollowed) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
            return convertToDtoList(
                    specialtyRepository.findAllByMemberIdOrderByIdDesc(studentId)
            );
        }
        throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
    }

    /**
     * 특기사항 등록 - 선생님 권한만
     */
    @Override
    @Transactional
    public void createSpecialty(Long studentId, SpecialtyForm form, LoginUserDto loginUser) {
        validateTeacherRole(loginUser);
        Member student = memberRepository.findById(studentId)
                .orElseThrow(() -> new ServiceException(ReturnCode.USER_NOT_FOUND));
        Specialty specialty = Specialty.builder()
                .member(student)
                .content(form.getContent())
                .build();
        specialtyRepository.save(specialty);
    }

    /**
     * 특기사항 수정 - 선생님 권한만
     */
    @Override
    @Transactional
    public void updateSpecialty(Long specialtyId, SpecialtyForm form, LoginUserDto loginUser) {
        validateTeacherRole(loginUser);
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ServiceException(ReturnCode.POST_NOT_FOUND));
        specialty.setContent(form.getContent());
    }

    /**
     * 특기사항 삭제 - 선생님 권한만
     */
    @Override
    @Transactional
    public void deleteSpecialty(Long specialtyId, LoginUserDto loginUser) {
        validateTeacherRole(loginUser);
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ServiceException(ReturnCode.POST_NOT_FOUND));
        specialtyRepository.delete(specialty);
    }

    // 특기사항 학년/학기별 조회 [학부모/선생님 권한]

    /**
     * 단건 조회 - 권한 제한 없음
     */
    @Override
    @Transactional(readOnly = true)
    public SpecialtyDto getSpecialty(Long specialtyId, LoginUserDto loginUser) {
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ServiceException(ReturnCode.POST_NOT_FOUND));

        return convertToDto(specialty);
    }
    /**
     * 권한 체크: 선생님만 허용
     */
    private void validateTeacherRole(LoginUserDto loginUser) {
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }

    /**
     * Specialty → DTO 변환
     */
    private SpecialtyDto convertToDto(Specialty specialty) {
        return SpecialtyDto.builder()
                .id(specialty.getId())
                .memberId(specialty.getMember().getId())
                .content(specialty.getContent())
                .build();
    }

    private List<SpecialtyDto> convertToDtoList(List<Specialty> specialties) {
        return specialties.stream()
                .map(this::convertToDto)
                .toList();
    }
}
