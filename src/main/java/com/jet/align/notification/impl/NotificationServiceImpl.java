package com.jet.align.notification.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.notification.NotificationService;
import com.jet.align.notification.PushSubscription;
import com.jet.align.notification.PushSubscriptionRepository;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int STATUS_NOT_FOUND = 404;
    private static final int STATUS_GONE = 410;

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushService pushService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void notify(User user, String title, String body) {
        String payload = buildPayload(title, body);
        for (PushSubscription subscription : pushSubscriptionRepository.findByUser(user)) {
            send(subscription, payload);
        }
    }

    private void send(PushSubscription subscription, String payload) {
        try {
            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    payload
            );
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == STATUS_NOT_FOUND || status == STATUS_GONE) {
                pushSubscriptionRepository.delete(subscription);
            }
        } catch (Exception e) {
            log.error("No se pudo enviar el push a la suscripción {} (endpoint={})",
                    subscription.getId(), subscription.getEndpoint(), e);
        }
    }

    private String buildPayload(String title, String body) {
        try {
            return objectMapper.writeValueAsString(Map.of("title", title, "body", body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del push", e);
        }
    }
}
