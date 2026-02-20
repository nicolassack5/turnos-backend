package com.nico.turnos.controller;

import com.nico.turnos.dto.EstadisticaDTO;
import com.nico.turnos.repository.TurnoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "http://localhost:5173")
public class EstadisticaController {

    private final TurnoRepository turnoRepository;

    public EstadisticaController(TurnoRepository turnoRepository) {
        this.turnoRepository = turnoRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, List<EstadisticaDTO>>> getDashboardData() {
        Map<String, List<EstadisticaDTO>> data = new HashMap<>();
        data.put("especialidades", turnoRepository.conteoPorEspecialidad());
        data.put("asistencia", turnoRepository.conteoPorAsistencia());
        return ResponseEntity.ok(data);
    }
}