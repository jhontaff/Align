package com.jet.align.notification;

import com.jet.align.notification.dto.SubscribeRequest;
import com.jet.align.user.User;

public interface PushSubscriptionService {
    void subscribe(User user, SubscribeRequest request);
    void unsubscribe(User user, String endpoint);
    String getVapidPublicKey();
}

