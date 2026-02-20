package com.nico.turnos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService { 

    @Value("${spring.ai.google.ai.api-key}")
    private String apiKey;

    private final RestClient restClient;

    public GeminiService(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.groq.com/openai/v1").build();
    }

    // Método 1: Para el reporte PDF (Le armamos un historial falso de 1 solo mensaje)
    public String preguntarAGemini(String pregunta) {
        List<Map<String, String>> unSoloMensaje = List.of(Map.of("role", "user", "content", pregunta));
        return preguntarAGemini(unSoloMensaje, "No hay contexto específico.");
    }

    // Método 2: Para el Chatbot (Recibe el historial completo)
    public String preguntarAGemini(List<Map<String, String>> historial, String contexto) {
        
        String systemPrompt = "Sos el asistente virtual de la Clínica Integral. Respondé de forma amable, natural, corta y útil en español. " +
                              "INFORMACIÓN IMPORTANTE PARA RESPONDER AL PACIENTE: " + contexto;

        // Armamos la lista de mensajes dinámica
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 1. Siempre ponemos las reglas del sistema primero
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        // 2. Agregamos todos los mensajes anteriores y el actual
        if (historial != null) {
            messages.addAll(historial);
        }

        var requestBody = Map.of(
            "model", "llama-3.1-8b-instant", 
            "messages", messages
        );

        try {
            var response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return "Sin respuesta del modelo.";
        } catch (Exception e) {
            System.err.println("❌ Error en Groq: " + e.getMessage());
            return "El asistente virtual no está disponible en este momento.";
        }
    }
}