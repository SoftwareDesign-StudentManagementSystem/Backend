package com.iEdu.domain.studentRecord.feedback.serviceImpl;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.repository.MemberRepository;
import com.iEdu.domain.studentRecord.feedback.dto.res.FeedbackDto;
import com.iEdu.domain.studentRecord.feedback.entity.Feedback;
import com.iEdu.domain.studentRecord.feedback.entity.FeedbackPage;
import com.iEdu.domain.studentRecord.feedback.repository.FeedbackRepository;
import com.iEdu.domain.studentRecord.feedback.service.FeedbackService;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;

    // 본인의 모든 피드백 조회 [학생 권한]
    @Override
    @Transactional
    public Page<FeedbackDto> getMyAllFeedback(Pageable pageable, LoginUserDto loginUser) {
        checkPageSize(pageable.getPageSize());
        // ROLE_STUDENT 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        return null;
    }

    // 학생의 모든 피드백 조회 [학부모/선생님 권한]
    @Override
    @Transactional
    public Page<FeedbackDto> getAllFeedback(Long studentId, Pageable pageable, LoginUserDto loginUser){
        checkPageSize(pageable.getPageSize());
        // ROLE_TEACHER 아닌 경우 예외 처리
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        return null;
    }

    // ----------------- 헬퍼 메서드 -----------------

    // 요청 페이지 수 제한
    private void checkPageSize(int pageSize) {
        int maxPageSize = FeedbackPage.getMaxPageSize();
        if (pageSize > maxPageSize) {
            throw new ServiceException(ReturnCode.PAGE_REQUEST_FAIL);
        }
    }

    // ROLE_STUDENT/ROLE_PARENT 아닌 경우 예외 처리
    private void validateAccessToFeedback(LoginUserDto loginUser) {
        Member.MemberRole role = loginUser.getRole();
        if (role != Member.MemberRole.ROLE_STUDENT && role != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
        // ROLE_PARENT인 경우 자녀가 있는지 확인
        if (role == Member.MemberRole.ROLE_PARENT) {
            Member parent = loginUser.ConvertToMember();
            boolean hasChildren = parent.getFollowList().stream()
                    .map(MemberFollow::getFollowed)
                    .anyMatch(Objects::nonNull);
            if (!hasChildren) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
        }
    }

    // Feedback -> FeedbackDto 변환

}
