package com.nico.turnos.service;

import com.nico.turnos.dto.TurnoRequest;
import com.nico.turnos.dto.TurnoResponse;
import com.nico.turnos.entity.Rol;
import com.nico.turnos.entity.Turno;
import com.nico.turnos.entity.Usuario;
import com.nico.turnos.exception.TurnoConflictException;
import com.nico.turnos.mapper.TurnoMapper;
import com.nico.turnos.repository.TurnoRepository;
import com.nico.turnos.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final TurnoMapper turnoMapper;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public TurnoService(TurnoRepository turnoRepository, 
                        TurnoMapper turnoMapper, 
                        UsuarioRepository usuarioRepository,
                        EmailService emailService) {
        this.turnoRepository = turnoRepository;
        this.turnoMapper = turnoMapper;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    // --- LISTAR TURNOS ---
    public List<TurnoResponse> listar() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Turno> turnos;

        if (usuario.getRol() == Rol.ADMIN) {
            turnos = turnoRepository.findAll();
        } else if (usuario.getRol() == Rol.MEDICO) {
            // Se filtran los turnos por el ID único del médico logueado
            turnos = turnoRepository.findByMedicoId(usuario.getId());
        } else {
            turnos = turnoRepository.findByPacienteUsername(username);
        }

        return turnos.stream()
                .map(turnoMapper::toResponse)
                .collect(Collectors.toList());
    }

    // --- CREAR TURNO ---
    public TurnoResponse crear(TurnoRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario paciente = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        // VALIDACIÓN DE SEGURIDAD: Solo pacientes crean turnos
        if (paciente.getRol() != Rol.PACIENTE) {
            throw new RuntimeException("Solo los pacientes pueden reservar turnos.");
        }

        // Validar que el horario esté entre 08:00 y 18:00
        validarHorarioNegocio(request.getFechaHora());

        // Validar que el médico no tenga otro turno en ese mismo momento
        if (turnoRepository.existsByMedicoIdAndFechaHora(request.getMedicoId(), request.getFechaHora())) {
            throw new TurnoConflictException("Este médico ya tiene un turno a esa hora.");
        }

        // Buscamos al médico en la tabla de usuarios para obtener sus datos reales
        Usuario medico = usuarioRepository.findById(request.getMedicoId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        Turno turno = turnoMapper.toEntity(request);
        
        // Seteamos los datos del paciente
        turno.setCliente(paciente.getNombreCompleto());
        turno.setPacienteUsername(paciente.getUsername());
        
        // Seteamos los datos del médico (incluyendo la especialidad para las gráficas)
        turno.setMedicoId(medico.getId()); 
        turno.setNombreMedico(medico.getNombreCompleto());
        turno.setEspecialidad(medico.getEspecialidad()); // <--- CRÍTICO PARA EL DASHBOARD

        Turno turnoGuardado = turnoRepository.save(turno);
        
        // Enviamos el email de confirmación
        enviarEmailConfirmacion(paciente.getUsername(), turnoGuardado);

        return turnoMapper.toResponse(turnoGuardado);
    }

    // --- ACTUALIZAR TURNO ---
    public TurnoResponse actualizar(Long id, TurnoRequest request) {
        Turno turnoExistente = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + id));

        // Si se cambia la fecha o el médico, validamos disponibilidad
        if (request.getFechaHora() != null && !turnoExistente.getFechaHora().equals(request.getFechaHora())) {
             validarHorarioNegocio(request.getFechaHora());
             
             Long idMedico = request.getMedicoId() != null ? request.getMedicoId() : turnoExistente.getMedicoId();
             
             if (turnoRepository.existsByMedicoIdAndFechaHoraAndIdNot(idMedico, request.getFechaHora(), id)) {
                 throw new TurnoConflictException("El médico ya tiene ocupado ese horario.");
             }
        }

        turnoMapper.actualizarTurno(turnoExistente, request);
        return turnoMapper.toResponse(turnoRepository.save(turnoExistente));
    }

    // --- ELIMINAR ---
    public void eliminar(Long id) {
        if (!turnoRepository.existsById(id)) {
            throw new RuntimeException("Turno no encontrado");
        }
        turnoRepository.deleteById(id);
    }

    // --- MÉTODOS PRIVADOS AUXILIARES ---

    private void validarHorarioNegocio(java.time.LocalDateTime fecha) {
        if (fecha == null) return;
        
        // No se atiende los domingos
        if (fecha.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            throw new RuntimeException("La clínica está cerrada los domingos.");
        }
        
        // Rango de 08:00 a 18:00
        int hora = fecha.getHour();
        if (hora < 8 || hora > 18) {
            throw new RuntimeException("El horario de atención es de 08:00 a 18:00.");
        }
    }

    private void enviarEmailConfirmacion(String emailDestino, Turno turno) {
        try {
            String fechaStr = turno.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String horaStr = turno.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm"));

            String asunto = "Confirmación de Turno - Clínica Integral";
            String mensaje = String.format(
                "Hola %s,\n\nTu turno ha sido reservado con éxito.\n\n" +
                "📅 Fecha: %s\n⏰ Hora: %s\n👨‍⚕️ Médico: %s\n🏥 Motivo: %s\n\n" +
                "Por favor, asiste puntual.\nSaludos, Clínica Integral.",
                turno.getCliente(), fechaStr, horaStr, turno.getNombreMedico(), turno.getDescripcion()
            );

            emailService.sendEmail(emailDestino, asunto, mensaje);
            System.out.println("📧 Email de confirmación enviado a: " + emailDestino);
        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
        }
    }
}