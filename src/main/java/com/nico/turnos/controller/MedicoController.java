package com.nico.turnos.controller;

import com.nico.turnos.entity.Medico;
import com.nico.turnos.repository.MedicoRepository;
import com.nico.turnos.repository.TurnoRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/medicos")
@CrossOrigin(origins = "http://localhost:5173") // Agregado para que no falle React
public class MedicoController {

    private final MedicoRepository medicoRepository;
    private final TurnoRepository turnoRepository;

    public MedicoController(MedicoRepository medicoRepository, TurnoRepository turnoRepository) {
        this.medicoRepository = medicoRepository;
        this.turnoRepository = turnoRepository;
    }

    @PostMapping
    public Medico crearMedico(@RequestBody Medico medico) {
        return medicoRepository.save(medico);
    }

    // 1. Listar todos los médicos
    @GetMapping
    public List<Medico> listarMedicos() {
        return medicoRepository.findAll();
    }

    // 2. Devuelve los horarios DISPONIBLES
    @GetMapping("/{id}/disponibilidad")
    public List<LocalTime> obtenerHorariosDisponibles(
            @PathVariable Long id,
            @RequestParam String fecha) { // YYYY-MM-DD

        LocalDate fechaConsultada = LocalDate.parse(fecha);

        // A. Definimos el horario de trabajo (9:00 a 17:00)
        LocalTime inicio = LocalTime.of(9, 0);
        LocalTime fin = LocalTime.of(17, 0);

        // B. Generamos slots cada 30 min
        List<LocalTime> slotsPosibles = new ArrayList<>();
        while (inicio.isBefore(fin)) {
            slotsPosibles.add(inicio);
            inicio = inicio.plusMinutes(30);
        }

        // C. Buscamos ocupados (Pasamos el ID y la FECHA DIRECTAMENTE)
        List<LocalDateTime> turnosOcupados = turnoRepository.findHorariosOcupados(id, fechaConsultada);

        // Convertimos a solo hora
        List<LocalTime> horariosOcupados = turnosOcupados.stream()
                .map(LocalDateTime::toLocalTime)
                .collect(Collectors.toList());

        // D. Restamos
        slotsPosibles.removeAll(horariosOcupados);

        return slotsPosibles;
    }
}