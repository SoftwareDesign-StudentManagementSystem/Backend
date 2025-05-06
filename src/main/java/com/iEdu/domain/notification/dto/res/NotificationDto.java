package com.iEdu.domain.notification.dto.res;

import com.iEdu.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private String content;
    private Boolean isRead;
    private Long objectId;
    private Notification.TargetObject targetObject;
}
