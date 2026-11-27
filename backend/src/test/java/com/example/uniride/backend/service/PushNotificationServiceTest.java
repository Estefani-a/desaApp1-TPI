package com.example.uniride.backend.service;

import com.example.uniride.backend.config.FirebaseConfig;
import com.example.uniride.backend.config.FirebaseProperties;
import com.example.uniride.backend.model.NotificationType;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.uniride.backend.TestData.NOW;
import static com.example.uniride.backend.TestData.passenger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FCM push delivery, payload contract (docs/API.md §9), stale-token cleanup,
 * and optional FirebaseConfig fallback.
 */
@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private PushGateway pushGateway;

    private PushNotificationService service;
    private User recipient;
    private PushNotificationEvent sampleEvent;

    @BeforeEach
    void setUp() {
        service = new PushNotificationService(userRepository, userService, pushGateway);
        recipient = passenger();
        sampleEvent = new PushNotificationEvent(
                NotificationType.SOLICITUD_ACEPTADA,
                recipient.getId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "¡Tenés lugar!",
                "Lucía F. aceptó tu solicitud para el viaje de las 22:15."
        );
    }

    @Test
    void enviaPushConPayloadDeDatosSegunContratoCuandoElUsuarioTieneToken() {
        recipient.updateFcmToken("fcm-token-valido", NOW);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(pushGateway.send(any())).thenReturn(PushGateway.SendResult.SENT);

        service.onPushNotification(sampleEvent);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushGateway).send(captor.capture());
        PushMessage sent = captor.getValue();
        assertThat(sent.fcmToken()).isEqualTo("fcm-token-valido");

        Map<String, String> payload = sent.toDataPayload();
        assertThat(payload).containsEntry("type", "SOLICITUD_ACEPTADA");
        assertThat(payload).containsEntry("tripId", sampleEvent.tripId().toString());
        assertThat(payload).containsEntry("reservationId", sampleEvent.reservationId().toString());
        assertThat(payload).containsEntry("title", "¡Tenés lugar!");
        assertThat(payload).containsEntry("body", sampleEvent.body());
        assertThat(payload).containsKey("notificationId");
        verifyNoInteractions(userService);
    }

    @Test
    void omiteEnvioCuandoElDestinatarioNoTieneTokenFcm() {
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));

        service.onPushNotification(sampleEvent);

        verifyNoInteractions(pushGateway);
        verifyNoInteractions(userService);
    }

    @Test
    void omiteEnvioCuandoElDestinatarioNoExiste() {
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.empty());

        service.onPushNotification(sampleEvent);

        verifyNoInteractions(pushGateway);
        verifyNoInteractions(userService);
    }

    @Test
    void limpiaTokenFcmObsoletoCuandoFcmRespondeUnregistered() {
        recipient.updateFcmToken("fcm-token-muerto", NOW);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(pushGateway.send(any())).thenReturn(PushGateway.SendResult.UNREGISTERED_TOKEN);

        service.onPushNotification(sampleEvent);

        verify(userService).clearStaleFcmToken(recipient.getId(), "fcm-token-muerto");
    }

    @Test
    void conservaTokenFcmAnteErrorTransitorioDeFcm() {
        recipient.updateFcmToken("fcm-token-valido", NOW);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(pushGateway.send(any())).thenReturn(PushGateway.SendResult.FAILED);

        service.onPushNotification(sampleEvent);

        verify(userService, never()).clearStaleFcmToken(any(), any());
    }

    @Test
    void fcmGatewayDevuelveUnregisteredCuandoFirebaseRechazaTokenDesregistrado() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
        when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(messaging.send(any(Message.class))).thenThrow(ex);

        FcmPushGateway gateway = new FcmPushGateway(messaging);
        PushMessage message = new PushMessage(
                UUID.randomUUID(),
                NotificationType.NUEVA_SOLICITUD,
                recipient.getId(),
                "token-muerto",
                sampleEvent.tripId(),
                sampleEvent.reservationId(),
                sampleEvent.title(),
                sampleEvent.body()
        );

        assertThat(gateway.send(message)).isEqualTo(PushGateway.SendResult.UNREGISTERED_TOKEN);
    }

    @Test
    void fcmGatewayDevuelveFailedAnteErrorTransitorioSinLanzarExcepcion() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
        when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        when(messaging.send(any(Message.class))).thenThrow(ex);

        FcmPushGateway gateway = new FcmPushGateway(messaging);
        PushMessage message = new PushMessage(
                UUID.randomUUID(),
                NotificationType.SOLICITUD_RECHAZADA,
                recipient.getId(),
                "token-valido",
                sampleEvent.tripId(),
                sampleEvent.reservationId(),
                sampleEvent.title(),
                sampleEvent.body()
        );

        assertThat(gateway.send(message)).isEqualTo(PushGateway.SendResult.FAILED);
    }

    @Test
    void firebaseConfigUsaLoggingGatewayCuandoNoHayRutaDeCredenciales() {
        FirebaseConfig config = new FirebaseConfig();

        PushGateway gateway = config.pushGateway(new FirebaseProperties(""));

        assertThat(gateway).isInstanceOf(LoggingPushGateway.class);
    }

    @Test
    void firebaseConfigFallaAlArrancarSiLaRutaDeCredencialesNoExiste() {
        FirebaseConfig config = new FirebaseConfig();

        assertThatThrownBy(() -> config.pushGateway(new FirebaseProperties("/ruta/inexistente/firebase.json")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIREBASE_CREDENTIALS_PATH");
    }
}
