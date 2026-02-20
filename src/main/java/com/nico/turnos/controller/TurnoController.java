package com.nico.turnos.controller;

import com.nico.turnos.dto.TurnoRequest;
import com.nico.turnos.dto.TurnoResponse;
import com.nico.turnos.entity.Turno;
import com.nico.turnos.repository.TurnoRepository;
import com.nico.turnos.service.ReporteService;
import com.nico.turnos.service.TurnoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/turnos")
@CrossOrigin(origins = "http://localhost:5173")
public class TurnoController {

    // Solo necesitamos estos 3 servicios ahora:
    private final TurnoService turnoService;
    private final ReporteService reporteService;
    private final TurnoRepository turnoRepository;

    // Constructor limpio (Borra los viejos emailService y usuarioRepository)
    public TurnoController(TurnoService turnoService, ReporteService reporteService, TurnoRepository turnoRepository) {
        this.turnoService = turnoService;
        this.reporteService = reporteService;
        this.turnoRepository = turnoRepository;
    }

    @GetMapping
    public List<TurnoResponse> listar() {
        return turnoService.listar();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TurnoRequest request) {
        try {
            TurnoResponse response = turnoService.crear(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody TurnoRequest request) {
        try {
            TurnoResponse response = turnoService.actualizar(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        turnoService.eliminar(id);
        return ResponseEntity.ok("Turno eliminado");
    }

    // --- NUEVO ENDPOINT PDF ---
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        byte[] pdfBytes = reporteService.generarReporteTurno(turno);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=turno_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}