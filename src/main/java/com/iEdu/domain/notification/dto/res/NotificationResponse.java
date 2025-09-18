package com.iEdu.domain.notification.dto.res;

import com.iEdu.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private String content;
    private Boolean isRead;
    private Long objectId;
    private Notification.TargetObject targetObject;
}
