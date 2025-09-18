package com.iEdu.domain.fcm.service;

import com.iEdu.domain.fcm.dto.res.FcmMessageResponse;

public interface FcmService {
    void sendMessageTo(FcmMessageResponse message);
}
