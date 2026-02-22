package com.nico.turnos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Tomamos el email real desde tu application.properties / Render
    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Ahora usa tu email real de forma dinámica
            message.setFrom("Clinica Integral <" + senderEmail + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            System.out.println("✅ ÉXITO: Email enviado correctamente a " + to);
            
        } catch (Exception e) {
            System.err.println("❌ ERROR FATAL AL ENVIAR EL EMAIL a " + to);
            e.printStackTrace(); // Esto va a imprimir el error exacto en Render
        }
    }
}