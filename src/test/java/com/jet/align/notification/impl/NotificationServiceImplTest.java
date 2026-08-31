package com.jet.align.notification.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.notification.PushSubscription;
import com.jet.align.notification.PushSubscriptionRepository;
import com.jet.align.user.User;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.security.Security;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // Clave pública EC (P-256) válida solo para que Notification pueda parsear un p256dh sintácticamente correcto.
    private static final String VALID_P256DH =
            "BMUHZ2Bc_fr0tp5VQLYQbxY0YV3o8tqs8BPmGY7S016fbo7jOAV4DI-x6zY414gR2-vF_J-FSMkgyM8j1rPfodg";

    private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
    private final PushService pushService = mock(PushService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationServiceImpl service =
            new NotificationServiceImpl(repository, pushService, objectMapper);
    private final User user = new User();

    private PushSubscription subscriptionOf(String endpoint) {
        PushSubscription subscription = new PushSubscription();
        subscription.setUser(user);
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(VALID_P256DH);
        subscription.setAuth("dGVzdC1hdXRoMTY");
        return subscription;
    }

    private HttpResponse responseWithStatus(int statusCode) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, statusCode, "");
    }

    @Test
    void notify_sin_suscripciones_no_llama_al_pushService() {
        when(repository.findByUser(user)).thenReturn(List.of());

        service.notify(user, "Título", "Cuerpo");

        verifyNoInteractions(pushService);
    }

    @Test
    void notify_envia_el_push_a_cada_suscripcion_del_usuario() throws Exception {
        PushSubscription subscription = subscriptionOf("https://push.example/1");
        when(repository.findByUser(user)).thenReturn(List.of(subscription));
        when(pushService.send(any(Notification.class))).thenReturn(responseWithStatus(201));

        service.notify(user, "Título", "Cuerpo");

        verify(pushService).send(any(Notification.class));
        verify(repository, never()).delete(any());
    }

    @Test
    void notify_borra_la_suscripcion_cuando_el_push_service_responde_410() throws Exception {
        PushSubscription subscription = subscriptionOf("https://push.example/1");
        when(repository.findByUser(user)).thenReturn(List.of(subscription));
        when(pushService.send(any(Notification.class))).thenReturn(responseWithStatus(410));

        service.notify(user, "Título", "Cuerpo");

        verify(repository).delete(subscription);
    }

    @Test
    void notify_borra_la_suscripcion_cuando_el_push_service_responde_404() throws Exception {
        PushSubscription subscription = subscriptionOf("https://push.example/1");
        when(repository.findByUser(user)).thenReturn(List.of(subscription));
        when(pushService.send(any(Notification.class))).thenReturn(responseWithStatus(404));

        service.notify(user, "Título", "Cuerpo");

        verify(repository).delete(subscription);
    }

    @Test
    void notify_no_borra_la_suscripcion_cuando_el_envio_es_exitoso() throws Exception {
        PushSubscription subscription = subscriptionOf("https://push.example/1");
        when(repository.findByUser(user)).thenReturn(List.of(subscription));
        when(pushService.send(any(Notification.class))).thenReturn(responseWithStatus(201));

        service.notify(user, "Título", "Cuerpo");

        verify(repository, never()).delete(any());
    }

    @Test
    void notify_continua_con_las_demas_suscripciones_si_una_falla() throws Exception {
        PushSubscription failing = subscriptionOf("https://push.example/failing");
        PushSubscription ok = subscriptionOf("https://push.example/ok");
        when(repository.findByUser(user)).thenReturn(List.of(failing, ok));
        when(pushService.send(any(Notification.class)))
                .thenThrow(new RuntimeException("network error"))
                .thenReturn(responseWithStatus(201));

        service.notify(user, "Título", "Cuerpo");

        verify(pushService, times(2)).send(any(Notification.class));
    }
}
