package com.nico.turnos.mapper;

import com.nico.turnos.dto.TurnoRequest;
import com.nico.turnos.dto.TurnoResponse;
import com.nico.turnos.entity.Turno;
import org.springframework.stereotype.Component;

@Component
public class TurnoMapper {

    // Request -> Entity
    public Turno toEntity(TurnoRequest request) {
        Turno turno = new Turno();
        turno.setMedicoId(request.getMedicoId());
        turno.setDescripcion(request.getDescripcion());
        turno.setFechaHora(request.getFechaHora());
        turno.setDiagnostico(request.getDiagnostico());
        turno.setAsistio(request.isAsistio());
        return turno;
    }

    // Entity -> Response
    public TurnoResponse toResponse(Turno turno) {
        TurnoResponse response = new TurnoResponse();
        response.setId(turno.getId());
        response.setCliente(turno.getCliente());
        response.setMedicoId(turno.getMedicoId());
        response.setNombreMedico(turno.getNombreMedico()); // Asegurate de setear esto en el Service
        response.setDescripcion(turno.getDescripcion());
        response.setFechaHora(turno.getFechaHora());
        response.setDiagnostico(turno.getDiagnostico());
        response.setAsistio(turno.isAsistio());
        return response;
    }

    // Método para actualizar una entidad existente con datos nuevos
    public void actualizarTurno(Turno turnoExistente, TurnoRequest request) {
        // Solo actualizamos lo que se permite cambiar
        turnoExistente.setDiagnostico(request.getDiagnostico());
        turnoExistente.setAsistio(request.isAsistio());
        
        // Si hay nueva fecha, la actualizamos
        if (request.getFechaHora() != null) {
            turnoExistente.setFechaHora(request.getFechaHora());
        }
        // Si cambia la descripción
        if (request.getDescripcion() != null) {
            turnoExistente.setDescripcion(request.getDescripcion());
        }
    }
}