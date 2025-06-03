package com.iEdu.domain.notification.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPageCacheDto {
    private List<NotificationDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
}
