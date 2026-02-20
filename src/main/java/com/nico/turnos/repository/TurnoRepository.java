package com.nico.turnos.repository;

import com.nico.turnos.dto.EstadisticaDTO;
import com.nico.turnos.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // Búsquedas básicas
    List<Turno> findByPacienteUsername(String pacienteUsername);
    List<Turno> findByMedicoId(Long medicoId);

    // Validaciones para evitar solapamiento de turnos
    boolean existsByMedicoIdAndFechaHora(Long medicoId, LocalDateTime fechaHora);
    
    // Validar solapamiento al editar (excluyendo el turno actual por ID)
    boolean existsByMedicoIdAndFechaHoraAndIdNot(Long medicoId, LocalDateTime fechaHora, Long id);

    // Query optimizada para calcular slots libres en el calendario
    @Query("SELECT t.fechaHora FROM Turno t WHERE t.medicoId = :medicoId AND CAST(t.fechaHora AS date) = :fecha")
    List<LocalDateTime> findHorariosOcupados(@Param("medicoId") Long medicoId, @Param("fecha") LocalDate fecha);

    // --- GRÁFICOS (RECHARTS) ---

    // Conteo de turnos por especialidad
    @Query("SELECT new com.nico.turnos.dto.EstadisticaDTO(t.especialidad, COUNT(t)) " +
           "FROM Turno t WHERE t.especialidad IS NOT NULL GROUP BY t.especialidad")
    List<EstadisticaDTO> conteoPorEspecialidad();

    // Conteo de Asistencias vs Ausencias
    @Query("SELECT new com.nico.turnos.dto.EstadisticaDTO(" +
           "CASE WHEN t.asistio = true THEN 'Asistió' ELSE 'Pendiente' END, COUNT(t)) " +
           "FROM Turno t GROUP BY t.asistio")
    List<EstadisticaDTO> conteoPorAsistencia();

    // 👇 NUEVO MÉTODO PARA BUSCAR TURNOS POR RANGO DE FECHAS
    java.util.List<com.nico.turnos.entity.Turno> findByFechaHoraBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}