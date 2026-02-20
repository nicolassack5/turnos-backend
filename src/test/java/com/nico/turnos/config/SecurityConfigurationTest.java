package com.nico.turnos.config;

import com.nico.turnos.controller.ChatController;
import com.nico.turnos.service.GeminiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ChatController.class})
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock external collaborators used by controllers/filters
    @MockBean
    private GeminiService geminiService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // to assert it's skipped on /chat

    @MockBean
    private AuthenticationProvider authenticationProvider; // required by Security config

    @MockBean
    private JwtService jwtService; // used by filter

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService; // used by filter

    @Test
    @DisplayName("Should allow unauthenticated POST to /chat/preguntar due to WebSecurityCustomizer and filter exclusion")
    void chatEndpoint_isPublic_andSkipsJwtFilter() throws Exception {
        Map<String, String> req = new HashMap<>();
        req.put("mensaje", "hola");
        Mockito.when(geminiService.preguntarAGemini(anyString())).thenReturn("respuesta");

        mockMvc.perform(post("/chat/preguntar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"hola\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.respuesta").value("respuesta"));

        // Since /chat/** is ignored and filter's shouldNotFilter returns true, doFilterInternal shouldn't be invoked
        verify(jwtAuthenticationFilter, never()).doFilterInternal(
                Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Should permit OPTIONS preflight without authentication on any path")
    void optionsPreflight_isPermitted() throws Exception {
        mockMvc.perform(options("/cualquier/ruta")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should enforce authentication for non-ignored, non-auth paths")
    void nonPublicPath_requiresAuth() throws Exception {
        mockMvc.perform(get("/medicos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should allow authenticated access to protected path")
    void protectedPath_withUser_isAllowed() throws Exception {
        mockMvc.perform(get("/medicos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should set CORS headers allowing origin http://localhost:5173")
    void cors_allowsConfiguredOrigin() throws Exception {
        Mockito.when(geminiService.preguntarAGemini(anyString())).thenReturn("ok");

        mockMvc.perform(post("/chat/preguntar")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"hola\"}"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(status().isOk());
    }
}
