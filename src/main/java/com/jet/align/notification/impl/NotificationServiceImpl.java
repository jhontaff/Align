package com.jet.align.notification.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.notification.NotificationService;
import com.jet.align.notification.PushSubscription;
import com.jet.align.notification.PushSubscriptionRepository;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
    public void notify(User user, String title, String body, String url) {
        String payload = buildPayload(title, body, url);
        List<PushSubscription> subs = pushSubscriptionRepository.findByUser(user);
        log.info("notify user={} subs={} title=\"{}\"", user.getId(), subs.size(), title);
        for (PushSubscription subscription : subs) {
            send(subscription, payload);
        }
    }

    private void send(PushSubscription subscription, String payload) {
        try {
            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    payload);
            HttpResponse response = pushService.send(notification, Encoding.AES128GCM);
            int status = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                    : "";

            if (status == STATUS_NOT_FOUND || status == STATUS_GONE) {
                log.warn("push {} -> {} (Gone); elimino suscripción", subscription.getId(), status);
                pushSubscriptionRepository.delete(subscription);
            } else if (status >= 300) {
                log.error("push {} -> {} body={}", subscription.getId(), status, responseBody);
            } else {
                log.info("push {} -> {}", subscription.getId(), status);
            }
        } catch (Exception e) {
            log.error("No se pudo enviar el push a la suscripción {} (endpoint={})",
                    subscription.getId(), subscription.getEndpoint(), e);
        }
    }

    private String buildPayload(String title, String body, String url) {
        try {
            Map<String, Object> notification = Map.of(
                    "title", title,
                    "body", body,
                    "icon", "/icons/icon-192x192.png",
                    "data", Map.of(
                            "url", url,
                            "onActionClick", Map.of(
                                    "default", Map.of(
                                            "operation", "focusLastFocusedOrOpen",
                                            "url", url))));
            return objectMapper.writeValueAsString(Map.of("notification", notification));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del push", e);
        }
    }
}
