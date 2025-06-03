package com.iEdu.domain.account.member.dto.res;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPageCacheDto {
    private List<MemberDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
}
