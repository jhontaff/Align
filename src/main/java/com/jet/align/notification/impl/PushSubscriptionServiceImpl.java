package com.jet.align.notification.impl;

import com.jet.align.notification.PushSubscription;
import com.jet.align.notification.PushSubscriptionRepository;
import com.jet.align.notification.PushSubscriptionService;
import com.jet.align.notification.dto.SubscribeRequest;
import com.jet.align.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PushSubscriptionServiceImpl implements PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final String vapidPublicKey;

    public PushSubscriptionServiceImpl(
            PushSubscriptionRepository pushSubscriptionRepository,
            @Value("${align.push.vapid.public-key}") String vapidPublicKey
    ) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.vapidPublicKey = vapidPublicKey;
    }

    @Override
    @Transactional
    public void subscribe(User user, SubscribeRequest request) {
        if (pushSubscriptionRepository.findByEndpoint(request.endpoint()).isPresent()) {
            return;
        }
        PushSubscription subscription = new PushSubscription();
        subscription.setUser(user);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.keys().p256dh());
        subscription.setAuth(request.keys().auth());
        pushSubscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void unsubscribe(User user, String endpoint) {
        pushSubscriptionRepository.deleteByUserAndEndpoint(user, endpoint);
    }

    @Override
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }
}
