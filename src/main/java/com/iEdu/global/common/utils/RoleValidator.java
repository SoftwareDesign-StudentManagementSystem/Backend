package com.iEdu.global.common.utils;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.mapper.MemberMapper;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleValidator {
    private final MemberMapper memberMapper;

    // ROLE_PARENT/ROLE_TEACHER 아닌 경우 예외 처리 + 자녀 확인
    public void validateAccessToStudent(LoginUserDto loginUser, Long studentId) {
        Member.MemberRole role = loginUser.getRole();

        if (role != Member.MemberRole.ROLE_PARENT && role != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }

        if (role == Member.MemberRole.ROLE_PARENT) {
            Member parent = memberMapper.toMember(loginUser);
            boolean isMyChild = parent.getFollowList().stream()
                    .map(MemberFollow::getFollowed)
                    .anyMatch(child -> child != null && child.getId().equals(studentId));
            if (!isMyChild) {
                throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
            }
        }
    }

    // ROLE_STUDENT 아닌 경우 예외 처리
    public void validateStudentRole(LoginUserDto loginUser) {
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }

    // ROLE_PARENT 아닌 경우 예외 처리
    public void validateParentRole(LoginUserDto loginUser) {
        if (loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }

    // ROLE_TEACHER 아닌 경우 예외 처리
    public void validateTeacherRole(LoginUserDto loginUser) {
        if (loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }

    // ROLE_ADMIN 아닌 경우 예외 처리
    public void validateAdminRole(LoginUserDto loginUser) {
        if (loginUser.getRole() != Member.MemberRole.ROLE_ADMIN) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }

    // ROLE_STUDENT/ROLE_PARENT 아닌 경우 예외 처리
    public void validateStudentOrParentRole(LoginUserDto loginUser) {
        if (loginUser.getRole() != Member.MemberRole.ROLE_STUDENT &&
                loginUser.getRole() != Member.MemberRole.ROLE_PARENT) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }

    // ROLE_PARENT이/ROLE_TEACHER 아닌 경우 예외 처리
    public void validateParentOrTeacherRole(LoginUserDto loginUser) {
        if (loginUser.getRole() != Member.MemberRole.ROLE_PARENT &&
                loginUser.getRole() != Member.MemberRole.ROLE_TEACHER) {
            throw new ServiceException(ReturnCode.NOT_AUTHORIZED);
        }
    }
}
