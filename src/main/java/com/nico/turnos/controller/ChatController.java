package com.nico.turnos.controller;

import com.nico.turnos.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "http://localhost:5173") 
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/preguntar")
    public ResponseEntity<Map<String, String>> preguntar(@RequestBody Map<String, Object> request) {
        Map<String, String> response = new HashMap<>();
        try {
            // Atrapamos el contexto y el historial (que ahora es una lista)
            String contexto = (String) request.getOrDefault("contexto", "No hay información del paciente.");
            
            @SuppressWarnings("unchecked")
            List<Map<String, String>> historial = (List<Map<String, String>>) request.get("historial");
            
            // Le pasamos el historial completo al servicio
            String respuestaIA = geminiService.preguntarAGemini(historial, contexto);
            
            response.put("respuesta", respuestaIA); 
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("respuesta", "Error del sistema: " + e.getMessage());
            return ResponseEntity.ok(response); 
        }
    }
}