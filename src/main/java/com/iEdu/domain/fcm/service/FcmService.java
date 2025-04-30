package com.iEdu.domain.fcm.service;

import com.iEdu.domain.fcm.dto.FcmMessage;

public interface FcmService {
    void sendMessageTo(FcmMessage message);
}
