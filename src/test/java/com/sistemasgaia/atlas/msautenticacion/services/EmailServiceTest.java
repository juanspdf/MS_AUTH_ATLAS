package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.exceptions.EmailSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para EmailService.
 * Verifica que los errores SMTP no devuelvan 200 OK.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Inyectar valores de @Value que no están disponibles fuera de Spring
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "fromName", "ATLAS Test");
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("Fallo SMTP no debe devolver 200 OK — debe lanzar EmailSendException")
    void falloSmtp_debeLanzarEmailSendException() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailSendException ex = assertThrows(
                EmailSendException.class,
                () -> emailService.enviarCorreoActivacion("test@test.com", "Test User", "token123"));

        assertTrue(ex.getMessage().contains("No se pudo enviar el correo"));
    }

    @Test
    @DisplayName("Envío exitoso no debe lanzar excepción")
    void envioExitoso_noDebeLanzarExcepcion() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        // mailSender.send() no lanza excepción → envío exitoso
        assertDoesNotThrow(
                () -> emailService.enviarCorreoActivacion("test@test.com", "Test User", "token123"));

        verify(mailSender).send(any(MimeMessage.class));
    }
}
