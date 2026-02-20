package com.nico.turnos.service;

import com.nico.turnos.entity.Turno;
import com.nico.turnos.repository.TurnoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class RecordatorioService {

    private final TurnoRepository turnoRepository;
    private final EmailService emailService;

    public RecordatorioService(TurnoRepository turnoRepository, EmailService emailService) {
        this.turnoRepository = turnoRepository;
        this.emailService = emailService;
    }

    // ⏰ SE EJECUTA TODOS LOS DÍAS A LAS 8:00 AM:
    // @Scheduled(cron = "0 0 8 * * ?") 
    
    // 🛠️ MODO DE PRUEBA: Para probarlo AHORA MISMO, usa esta línea en vez de la de arriba.
    // Esto hace que el robot se despierte CADA 1 MINUTO y mande los mails:
    @Scheduled(fixedRate = 60000)
    public void enviarRecordatoriosDiarios() {
        System.out.println("⏳ [CRON] Buscando turnos para enviar recordatorios de mañana...");

        // 1. Calculamos el día de mañana (desde las 00:00 hasta las 23:59)
        LocalDate manana = LocalDate.now().plusDays(1);
        LocalDateTime inicioDia = manana.atStartOfDay();
        LocalDateTime finDia = manana.atTime(LocalTime.MAX);

        // 2. Buscamos los turnos en la base de datos
        List<Turno> turnosManana = turnoRepository.findByFechaHoraBetween(inicioDia, finDia);

        if (turnosManana.isEmpty()) {
            System.out.println("💤 No hay turnos para mañana. Sigo durmiendo.");
            return;
        }

        // 3. Mandamos los mails a cada paciente
        for (Turno turno : turnosManana) {
            if (turno.getPacienteUsername() != null && !turno.getPacienteUsername().isEmpty() && turno.getPacienteUsername().contains("@")) {
                
                String emailPaciente = turno.getPacienteUsername();
                String asunto = "⏰ Recordatorio de Turno - Clínica Integral";
                
                String horaTurno = turno.getFechaHora().toLocalTime().toString();
                String mensaje = "Hola " + turno.getCliente() + ",\n\n"
                        + "Te recordamos que tenés un turno mañana (" + manana + ") a las " + horaTurno + " hs "
                        + "con el Dr./Dra. " + turno.getNombreMedico() + ".\n\n"
                        + "Especialidad: " + turno.getEspecialidad() + "\n"
                        + "Motivo: " + turno.getDescripcion() + "\n\n"
                        + "Por favor, recordá asistir 10 minutos antes a la clínica.\n\n"
                        + "Saludos,\nTu Asistente Virtual de Clínica Integral.";

                try {
                    emailService.sendEmail(emailPaciente, asunto, mensaje);
                    System.out.println("✅ Recordatorio enviado con éxito a: " + emailPaciente);
                } catch (Exception e) {
                    System.err.println("❌ Error enviando correo a " + emailPaciente + ": " + e.getMessage());
                }
            }
        }
    }
}