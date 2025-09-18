package com.iEdu.domain.account.member.mapper;

import com.iEdu.domain.account.auth.loginUser.LoginUserDto;
import com.iEdu.domain.account.member.dto.res.DetailMemberDto;
import com.iEdu.domain.account.member.dto.res.MemberDto;
import com.iEdu.domain.account.member.dto.res.SimpleMember;
import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.domain.account.member.entity.MemberFollow;
import com.iEdu.domain.account.member.entity.MemberFollowReq;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberMapper {
    // ---------- Member / LoginUserDto -> MemberDto ----------
    MemberDto toMemberDto(Member source);
    MemberDto toMemberDto(LoginUserDto source);

    // ---------- Member / LoginUserDto -> DetailMemberDto ----------
    @Mappings({
            @Mapping(target = "childrenList", source = "followList", qualifiedByName = "followToChildren"),
            @Mapping(target = "parentList",   source = "followedList", qualifiedByName = "followedToParents"),
            @Mapping(target = "followReqList", source = "followReqList", qualifiedByName = "followReqToRecs"),
            @Mapping(target = "followRecList", source = "followRecList", qualifiedByName = "followReqToReqs")
    })
    DetailMemberDto toDetailMemberDto(Member source);

    @Mappings({
            @Mapping(target = "childrenList", source = "followList", qualifiedByName = "followToChildren"),
            @Mapping(target = "parentList",   source = "followedList", qualifiedByName = "followedToParents"),
            @Mapping(target = "followReqList", source = "followReqList", qualifiedByName = "followReqToRecs"),
            @Mapping(target = "followRecList", source = "followRecList", qualifiedByName = "followReqToReqs")
    })
    DetailMemberDto toDetailMemberDto(LoginUserDto source);

    // ---------- LoginUserDto -> Member (엔티티 저장용) ----------
    // 관계 필드나 콜렉션은 무시 (지연 로딩 유발/불필요한 merge 방지)
    @Mappings({
            @Mapping(target = "followList", ignore = true),
            @Mapping(target = "followedList", ignore = true),
            @Mapping(target = "followReqList", ignore = true),
            @Mapping(target = "followRecList", ignore = true)
    })
    Member toMember(LoginUserDto source);

    // ---------- Member -> LoginUserDto ----------
    // 관계 필드나 콜렉션은 무시 (지연 로딩 유발/불필요한 merge 방지)
    @Mappings({
            @Mapping(target = "followList", ignore = true),
            @Mapping(target = "followedList", ignore = true),
            @Mapping(target = "followReqList", ignore = true),
            @Mapping(target = "followRecList", ignore = true)
    })
    LoginUserDto toLoginUserDto(Member source);

    // ---------- List & Element 변환기 ----------

    @Named("followToChildren")
    default List<SimpleMember> followToChildren(List<MemberFollow> follows) {
        if (follows == null) return List.of();
        return follows.stream()
                .map(this::followToChild)
                .collect(Collectors.toList());
    }

    @Named("followedToParents")
    default List<SimpleMember> followedToParents(List<MemberFollow> followedList) {
        if (followedList == null) return List.of();
        return followedList.stream()
                .map(this::followedToParent)
                .collect(Collectors.toList());
    }

    @Named("followReqToRecs")
    default List<SimpleMember> followReqToRecs(List<MemberFollowReq> reqs) {
        if (reqs == null) return List.of();
        return reqs.stream()
                .map(this::followReqToRec)
                .collect(Collectors.toList());
    }

    @Named("followReqToReqs")
    default List<SimpleMember> followReqToReqs(List<MemberFollowReq> reqs) {
        if (reqs == null) return List.of();
        return reqs.stream()
                .map(this::followReqToRequester)
                .collect(Collectors.toList());
    }

    // ---------- Element 단위 매핑(팔로우/요청) ----------

    // 자녀: MemberFollow.followed 기준
    default SimpleMember followToChild(MemberFollow mf) {
        if (mf == null || mf.getFollowed() == null) return null;
        var m = mf.getFollowed();
        return SimpleMember.builder()
                .id(m.getId())
                .name(m.getName())
                .profileImageUrl(m.getProfileImageUrl())
                .year(m.getYear())
                .classId(m.getClassId())
                .number(m.getNumber())
                .role(m.getRole())
                .build();
    }

    // 부모: MemberFollow.follow 기준
    default SimpleMember followedToParent(MemberFollow mf) {
        if (mf == null || mf.getFollow() == null) return null;
        var m = mf.getFollow();
        return SimpleMember.builder()
                .id(m.getId())
                .name(m.getName())
                .profileImageUrl(m.getProfileImageUrl())
                .year(m.getYear())
                .classId(m.getClassId())
                .number(m.getNumber())
                .role(m.getRole())
                .build();
    }

    // 내가 요청한 팔로우 목록: MemberFollowReq.followRec 기준
    default SimpleMember followReqToRec(MemberFollowReq req) {
        if (req == null || req.getFollowRec() == null) return null;
        var m = req.getFollowRec();
        return SimpleMember.builder()
                .id(m.getId())
                .name(m.getName())
                .profileImageUrl(m.getProfileImageUrl())
                .year(m.getYear())
                .classId(m.getClassId())
                .number(m.getNumber())
                .role(m.getRole())
                .build();
    }

    // 내가 받은 팔로우 요청 목록: MemberFollowReq.followReq 기준
    default SimpleMember followReqToRequester(MemberFollowReq req) {
        if (req == null || req.getFollowReq() == null) return null;
        var m = req.getFollowReq();
        return SimpleMember.builder()
                .id(m.getId())
                .name(m.getName())
                .profileImageUrl(m.getProfileImageUrl())
                .year(m.getYear())
                .classId(m.getClassId())
                .number(m.getNumber())
                .role(m.getRole())
                .build();
    }
}
