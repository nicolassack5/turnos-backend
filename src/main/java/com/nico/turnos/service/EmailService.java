package com.nico.turnos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${BREVO_API_KEY}")
    private String apiKey;

    @Value("${BREVO_SENDER_EMAIL}")
    private String senderEmail;

    private final RestClient restClient;

    public EmailService(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.brevo.com/v3").build();
    }

    public void sendEmail(String to, String subject, String content) {
        try {
            // Convertimos los saltos de línea a formato HTML para que se vea lindo
            String htmlContent = "<p>" + content.replace("\n", "<br>") + "</p>";

            Map<String, Object> requestBody = Map.of(
                "sender", Map.of("name", "Clínica Integral", "email", senderEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlContent
            );

            restClient.post()
                .uri("/smtp/email")
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

            System.out.println("✅ ÉXITO: Email enviado a " + to + " vía Brevo API");

        } catch (Exception e) {
            System.err.println("❌ ERROR AL ENVIAR EMAIL a " + to + ": " + e.getMessage());
        }
    }
}