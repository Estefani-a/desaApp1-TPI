package com.example.uniride.backend.service;

import com.example.uniride.backend.config.AppLinkProperties;
import com.example.uniride.backend.model.AuthTokenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sends transactional emails for RF01 (account activation and password reset) via SMTP / Mailpit.
 */
@Service
public class AuthMailService {

    private static final Logger log = LoggerFactory.getLogger(AuthMailService.class);

    private final JavaMailSender mailSender;
    private final AppLinkProperties linkProperties;

    public AuthMailService(JavaMailSender mailSender, AppLinkProperties linkProperties) {
        this.mailSender = mailSender;
        this.linkProperties = linkProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuthEmail(AuthEmailEvent event) {
        switch (event.type()) {
            case ACTIVATION -> sendActivationEmail(event.toEmail(), event.fullName(), event.rawToken());
            case PASSWORD_RESET -> sendPasswordResetEmail(event.toEmail(), event.fullName(), event.rawToken());
        }
    }

    public void sendActivationEmail(String toEmail, String fullName, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String link = linkProperties.normalizedBaseUrl() + "/api/v1/links/activate?token=" + encodedToken;
        String subject = "Activá tu cuenta de UniRide";
        String body = """
                Hola %s,

                Para activar tu cuenta institucional en UniRide, abrí el siguiente enlace (válido por %s):

                %s

                Si no creaste esta cuenta, podés ignorar este correo.
                """.formatted(fullName, AuthTokenType.ACTIVATION.humanDuration(), link);

        send(toEmail, subject, body);
    }

    public void sendPasswordResetEmail(String toEmail, String fullName, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String link = linkProperties.normalizedBaseUrl() + "/api/v1/links/reset-password?token=" + encodedToken;
        String subject = "Restablecé tu contraseña de UniRide";
        String body = """
                Hola %s,

                Recibimos un pedido para restablecer tu contraseña en UniRide. Abrí el siguiente enlace (válido por %s):

                %s

                Si no solicitaste este cambio, podés ignorar este correo.
                """.formatted(fullName, AuthTokenType.PASSWORD_RESET.humanDuration(), link);

        send(toEmail, subject, body);
    }

    private void send(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(linkProperties.mailFrom());
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("No se pudo enviar el correo '{}' a {}: {}", subject, toEmail, ex.getMessage());
        }
    }
}
