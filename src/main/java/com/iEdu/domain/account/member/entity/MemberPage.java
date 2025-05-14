package com.iEdu.domain.account.member.entity;

import lombok.Data;
import lombok.Getter;

@Data
public class MemberPage {
    // 기본 page, size
    private int page = 0;
    private int size = 25;
    @Getter
    private static final int maxPageSize = 25;
}
