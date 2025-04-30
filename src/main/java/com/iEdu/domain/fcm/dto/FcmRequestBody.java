package com.iEdu.domain.fcm.dto;

import lombok.Getter;

@Getter
public class FcmRequestBody {
    private final String to;
    private final Notification notification;

    public FcmRequestBody(FcmMessage message) {
        this.to = message.getTargetToken();
        this.notification = new Notification(message.getTitle(), message.getBody());
    }
    @Getter
    public static class Notification {
        private final String title;
        private final String body;

        public Notification(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }
}
