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

    public List<TurnoResponse> listar() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Turno> turnos;

        if (usuario.getRol() == Rol.ADMIN) {
            turnos = turnoRepository.findAll();
        } else if (usuario.getRol() == Rol.MEDICO) {
            turnos = turnoRepository.findByMedicoId(usuario.getId());
        } else {
            turnos = turnoRepository.findByPacienteUsername(username);
        }

        return turnos.stream()
                .map(turnoMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TurnoResponse crear(TurnoRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario paciente = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        if (paciente.getRol() != Rol.PACIENTE) {
            throw new RuntimeException("Solo los pacientes pueden reservar turnos.");
        }

        validarHorarioNegocio(request.getFechaHora());

        if (turnoRepository.existsByMedicoIdAndFechaHora(request.getMedicoId(), request.getFechaHora())) {
            throw new TurnoConflictException("Este médico ya tiene un turno a esa hora.");
        }

        Usuario medico = usuarioRepository.findById(request.getMedicoId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        Turno turno = turnoMapper.toEntity(request);
        
        turno.setCliente(paciente.getNombreCompleto());
        turno.setPacienteUsername(paciente.getUsername());
        turno.setMedicoId(medico.getId()); 
        turno.setNombreMedico(medico.getNombreCompleto());
        turno.setEspecialidad(medico.getEspecialidad()); 

        Turno turnoGuardado = turnoRepository.save(turno);
        
        // 👇 ACÁ VOLVIMOS A ACTIVAR EL EMAIL
        enviarEmailConfirmacion(paciente.getUsername(), turnoGuardado);

        return turnoMapper.toResponse(turnoGuardado);
    }

    public TurnoResponse actualizar(Long id, TurnoRequest request) {
        Turno turnoExistente = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + id));

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

    public void eliminar(Long id) {
        if (!turnoRepository.existsById(id)) {
            throw new RuntimeException("Turno no encontrado");
        }
        turnoRepository.deleteById(id);
    }

    private void validarHorarioNegocio(java.time.LocalDateTime fecha) {
        if (fecha == null) return;
        
        if (fecha.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            throw new RuntimeException("La clínica está cerrada los domingos.");
        }
        
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
                "Por favor, recuerda asistir 10 minutos antes.\nSaludos,\nClínica Integral.",
                turno.getCliente(), fechaStr, horaStr, turno.getNombreMedico(), turno.getDescripcion()
            );

            emailService.sendEmail(emailDestino, asunto, mensaje);
            System.out.println("📧 Email de confirmación enviado a: " + emailDestino);
        } catch (Exception e) {
            System.err.println("Error enviando email de confirmación: " + e.getMessage());
        }
    }
}