package com.sistemasgaia.atlas.msautenticacion.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${atlas.email.from:noreply@sistemasgaia.com}")
    private String fromEmail;

    @Value("${atlas.email.from-name:Sistema ATLAS}")
    private String fromName;

    @Value("${atlas.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Async
    public void enviarCorreoActivacion(String destinatario, String nombreCompleto, String token) {
        String enlace = frontendBaseUrl + "/activar-cuenta?token=" + token;
        String asunto = "ATLAS - Activa tu cuenta";
        String html = buildActivacionHtml(nombreCompleto, enlace);
        enviarCorreoHtml(destinatario, asunto, html);
        log.info("Correo de activacion enviado a: {}", destinatario);
    }

    @Async
    public void enviarCorreoRecuperacion(String destinatario, String nombreCompleto, String token) {
        String enlace = frontendBaseUrl + "/restablecer-contrasenia?token=" + token;
        String asunto = "ATLAS - Restablece tu contrasenia";
        String html = buildRecuperacionHtml(nombreCompleto, enlace);
        enviarCorreoHtml(destinatario, asunto, html);
        log.info("Correo de recuperacion enviado a: {}", destinatario);
    }

    private void enviarCorreoHtml(String destinatario, String asunto, String contenidoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoHtml, true);
            mailSender.send(mensaje);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Error enviando correo a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar correo electronico", e);
        }
    }

    private String buildActivacionHtml(String nombre, String enlace) {
        return "<html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Segoe UI,sans-serif;'>"
             + "<div style='max-width:600px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.08);'>"
             + "<div style='background:linear-gradient(135deg,#1a237e,#3949ab);padding:40px 30px;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;font-size:28px;'>ATLAS</h1>"
             + "<p style='color:#b3c6ff;margin:8px 0 0;font-size:14px;'>Sistema de Autenticacion</p></div>"
             + "<div style='padding:40px 30px;'>"
             + "<h2 style='color:#1a237e;margin:0 0 20px;'>Bienvenido, " + nombre + "!</h2>"
             + "<p style='color:#424242;font-size:15px;line-height:1.7;'>Tu cuenta ha sido creada en <b>ATLAS</b>. Para completar tu registro, establece tu contrasenia:</p>"
             + "<div style='text-align:center;margin:30px 0;'>"
             + "<a href='" + enlace + "' style='display:inline-block;background:linear-gradient(135deg,#1a237e,#3949ab);color:#fff;text-decoration:none;padding:14px 40px;border-radius:8px;font-size:16px;font-weight:600;'>Activar mi cuenta</a></div>"
             + "<div style='background:#fff8e1;border-left:4px solid #ffc107;padding:15px;border-radius:0 6px 6px 0;margin:25px 0;'>"
             + "<p style='color:#6d4c00;font-size:13px;margin:0;'><b>Importante:</b> Este enlace expira en <b>24 horas</b>. Si no solicitaste esta cuenta, ignora este correo.</p></div>"
             + "<p style='color:#757575;font-size:12px;'>Si el boton no funciona: <a href='" + enlace + "' style='color:#3949ab;word-break:break-all;'>" + enlace + "</a></p></div>"
             + "<div style='background:#f5f5f5;padding:20px 30px;text-align:center;border-top:1px solid #e0e0e0;'>"
             + "<p style='color:#9e9e9e;font-size:12px;margin:0;'>2026 Sistemas Gaia - Sistema ATLAS. Correo automatico.</p></div>"
             + "</div></body></html>";
    }

    private String buildRecuperacionHtml(String nombre, String enlace) {
        return "<html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Segoe UI,sans-serif;'>"
             + "<div style='max-width:600px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.08);'>"
             + "<div style='background:linear-gradient(135deg,#b71c1c,#e53935);padding:40px 30px;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;font-size:28px;'>ATLAS</h1>"
             + "<p style='color:#ffcdd2;margin:8px 0 0;font-size:14px;'>Recuperacion de contrasenia</p></div>"
             + "<div style='padding:40px 30px;'>"
             + "<h2 style='color:#b71c1c;margin:0 0 20px;'>Hola, " + nombre + "</h2>"
             + "<p style='color:#424242;font-size:15px;line-height:1.7;'>Recibimos una solicitud para restablecer la contrasenia de tu cuenta en <b>ATLAS</b>.</p>"
             + "<div style='text-align:center;margin:30px 0;'>"
             + "<a href='" + enlace + "' style='display:inline-block;background:linear-gradient(135deg,#b71c1c,#e53935);color:#fff;text-decoration:none;padding:14px 40px;border-radius:8px;font-size:16px;font-weight:600;'>Restablecer contrasenia</a></div>"
             + "<div style='background:#fce4ec;border-left:4px solid #e53935;padding:15px;border-radius:0 6px 6px 0;margin:25px 0;'>"
             + "<p style='color:#6d0000;font-size:13px;margin:0;'><b>Seguridad:</b> Este enlace expira en <b>24 horas</b> y solo puede usarse una vez.</p></div>"
             + "<p style='color:#757575;font-size:12px;'>Si el boton no funciona: <a href='" + enlace + "' style='color:#e53935;word-break:break-all;'>" + enlace + "</a></p></div>"
             + "<div style='background:#f5f5f5;padding:20px 30px;text-align:center;border-top:1px solid #e0e0e0;'>"
             + "<p style='color:#9e9e9e;font-size:12px;margin:0;'>2026 Sistemas Gaia - Sistema ATLAS. Correo automatico.</p></div>"
             + "</div></body></html>";
    }
}
