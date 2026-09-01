package com.jet.align.notification.impl;

import com.jet.align.notification.PushSubscription;
import com.jet.align.notification.PushSubscriptionRepository;
import com.jet.align.notification.dto.SubscribeRequest;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PushSubscriptionServiceImplTest {

    private static final String VAPID_PUBLIC_KEY = "test-vapid-public-key";

    private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
    private final PushSubscriptionServiceImpl service =
            new PushSubscriptionServiceImpl(repository, VAPID_PUBLIC_KEY);
    private final User user = new User();

    private SubscribeRequest requestFor(String endpoint) {
        return new SubscribeRequest(endpoint, new SubscribeRequest.Keys("p256dh-value", "auth-value"));
    }

    @Test
    void subscribe_guarda_una_suscripcion_nueva() {
        SubscribeRequest request = requestFor("https://push.example/1");
        when(repository.findByEndpoint(request.endpoint())).thenReturn(Optional.empty());

        service.subscribe(user, request);

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(repository).save(captor.capture());
        PushSubscription saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getEndpoint()).isEqualTo(request.endpoint());
        assertThat(saved.getP256dh()).isEqualTo(request.keys().p256dh());
        assertThat(saved.getAuth()).isEqualTo(request.keys().auth());
    }

    @Test
    void subscribe_del_mismo_usuario_reusa_la_fila_existente_sin_crear_una_nueva() {
        SubscribeRequest request = requestFor("https://push.example/1");
        PushSubscription existing = new PushSubscription();
        existing.setUser(user);
        existing.setEndpoint(request.endpoint());
        when(repository.findByEndpoint(request.endpoint())).thenReturn(Optional.of(existing));

        service.subscribe(user, request);

        // Idempotente en efecto: guarda sobre la MISMA entidad, no una fila nueva.
        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void subscribe_reasigna_el_endpoint_cuando_pertenece_a_otro_usuario() {
        SubscribeRequest request = requestFor("https://push.example/1");
        User previousOwner = new User();
        PushSubscription existing = new PushSubscription();
        existing.setUser(previousOwner);
        existing.setEndpoint(request.endpoint());
        when(repository.findByEndpoint(request.endpoint())).thenReturn(Optional.of(existing));

        service.subscribe(user, request);

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(repository).save(captor.capture());
        PushSubscription saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getP256dh()).isEqualTo(request.keys().p256dh());
        assertThat(saved.getAuth()).isEqualTo(request.keys().auth());
    }

    @Test
    void unsubscribe_delega_en_el_repositorio() {
        service.unsubscribe(user, "https://push.example/1");

        verify(repository).deleteByUserAndEndpoint(user, "https://push.example/1");
    }

    @Test
    void getVapidPublicKey_devuelve_la_clave_inyectada() {
        assertThat(service.getVapidPublicKey()).isEqualTo(VAPID_PUBLIC_KEY);
    }
}
