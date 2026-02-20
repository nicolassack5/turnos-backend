package com.nico.turnos.controller;

import com.nico.turnos.entity.Medico;
import com.nico.turnos.repository.MedicoRepository;
import com.nico.turnos.repository.TurnoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicoController.class)
class MedicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MedicoRepository medicoRepository;

    @MockBean
    private TurnoRepository turnoRepository;

    @Test
    @WithMockUser
    @DisplayName("Should create a doctor via POST /medicos and return persisted entity")
    void crearMedico_returnsSavedEntity() throws Exception {
        Medico input = new Medico("Dr. Test", "Cardiología", "test@example.com");
        input.setId(1L);
        given(medicoRepository.save(any(Medico.class))).willReturn(input);

        String body = "{\n" +
                "  \"nombre\": \"Dr. Test\",\n" +
                "  \"especialidad\": \"Cardiología\",\n" +
                "  \"email\": \"test@example.com\"\n" +
                "}";

        mockMvc.perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nombre", is("Dr. Test")))
                .andExpect(jsonPath("$.especialidad", is("Cardiología")))
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should list all doctors via GET /medicos")
    void listarMedicos_returnsAll() throws Exception {
        Medico m1 = new Medico("Dr. A", "Cardiología", "a@ex.com"); m1.setId(1L);
        Medico m2 = new Medico("Dr. B", "Dermatología", "b@ex.com"); m2.setId(2L);
        given(medicoRepository.findAll()).willReturn(Arrays.asList(m1, m2));

        mockMvc.perform(get("/medicos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre", is("Dr. A")))
                .andExpect(jsonPath("$[1].especialidad", is("Dermatología")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return all 30-min slots between 09:00 and 17:00 when no bookings exist")
    void disponibilidad_noBookings_returnsAllSlots() throws Exception {
        Long medicoId = 5L;
        String fecha = "2025-01-10";
        given(turnoRepository.findHorariosOcupados(eq(medicoId), eq(LocalDate.parse(fecha))))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/medicos/{id}/disponibilidad", medicoId)
                        .param("fecha", fecha))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0]", is("09:00:00")))
                .andExpect(jsonPath("$[15]", is("16:30:00")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should exclude occupied times returned by repository from availability list")
    void disponibilidad_excludesOccupied() throws Exception {
        Long medicoId = 7L;
        String fecha = "2025-02-01";
        List<LocalDateTime> ocupados = Arrays.asList(
                LocalDateTime.of(2025, 2, 1, 9, 0),
                LocalDateTime.of(2025, 2, 1, 10, 30),
                LocalDateTime.of(2025, 2, 1, 16, 30)
        );
        given(turnoRepository.findHorariosOcupados(eq(medicoId), eq(LocalDate.parse(fecha))))
                .willReturn(ocupados);

        mockMvc.perform(get("/medicos/{id}/disponibilidad", medicoId)
                        .param("fecha", fecha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(13)))
                .andExpect(jsonPath("$", not(hasItems("09:00:00", "10:30:00", "16:30:00"))))
                .andExpect(jsonPath("$", hasItems("09:30:00", "10:00:00", "11:00:00")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should parse date from query param and call repository with correct LocalDate")
    void disponibilidad_parsesDateAndCallsRepo() throws Exception {
        Long medicoId = 9L;
        String fecha = "2026-03-15";
        given(turnoRepository.findHorariosOcupados(eq(medicoId), eq(LocalDate.parse(fecha))))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/medicos/{id}/disponibilidad", medicoId)
                        .param("fecha", fecha))
                .andExpect(status().isOk());
    }
}
