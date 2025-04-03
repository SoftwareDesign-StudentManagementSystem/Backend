package com.iEdu.global.common.response;

import lombok.Getter;

import java.util.List;

@Getter
public class SwDesignPage<T> {
    private List<T> contents;

    private int pageNumber;
    private int pageSize;
    private int totalPages;
    private long totalCount;

    public static <T> SwDesignPage<T> of(org.springframework.data.domain.Page<T> pagedContents) {
        SwDesignPage<T> converted = new SwDesignPage<>();
        converted.contents = pagedContents.getContent();
        converted.pageNumber = pagedContents.getNumber();
        converted.pageSize = pagedContents.getSize();
        converted.totalPages = pagedContents.getTotalPages();
        converted.totalCount = pagedContents.getTotalElements();
        return converted;
    }
}

