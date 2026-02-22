package com.nico.turnos.controller;

import com.nico.turnos.config.JwtService;
import com.nico.turnos.dto.AuthResponse;
import com.nico.turnos.dto.LoginRequest;
import com.nico.turnos.dto.RegisterRequest;
import com.nico.turnos.entity.Rol;
import com.nico.turnos.entity.Usuario;
import com.nico.turnos.repository.UsuarioRepository;
import com.nico.turnos.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
// Eliminamos el @CrossOrigin con localhost porque ya lo maneja SecurityConfiguration
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthController(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Credenciales incorrectas o cuenta no verificada.");
        }

        Usuario user = usuarioRepository.findByUsername(request.getUsername()).orElseThrow();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getRol().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("El usuario ya existe");
        }

        Usuario user = new Usuario();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNombreCompleto(request.getNombreCompleto());
        user.setDni(request.getDni());
        user.setTelefono(request.getTelefono());
        user.setRol(request.getRol());
        user.setEspecialidad(request.getEspecialidad());
        
        // 👇 CAMBIO 1: TODOS LOS USUARIOS NACEN ACTIVADOS PARA SALTEAR EL EMAIL
        user.setEnabled(true); 
        
        if (request.getRol() == Rol.PACIENTE) {
            String token = UUID.randomUUID().toString();
            user.setVerificationCode(token);
            usuarioRepository.save(user);

            // 👇 CAMBIO 2: APAGAMOS EL ENVÍO DE EMAIL
            // String link = "https://turnos-frontend-khaki.vercel.app?verifyToken=" + token;
            // emailService.sendEmail(request.getUsername(), "Verifica tu cuenta - Clínica Integral",
            //        "Hola " + request.getNombreCompleto() + ",\n\nActiva tu cuenta aquí: " + link);
            
            // Cambiamos el mensaje para que el usuario sepa que ya puede entrar
            return ResponseEntity.ok("Registro exitoso. Ya puedes iniciar sesión.");
        } else {
            usuarioRepository.save(user);
            return ResponseEntity.ok("Usuario " + request.getRol() + " creado y activado correctamente.");
        }
    }

    @GetMapping("/verify-account")
    public ResponseEntity<?> verifyAccount(@RequestParam String token) {
        Usuario usuario = usuarioRepository.findAll().stream()
                .filter(u -> token.equals(u.getVerificationCode()))
                .findFirst()
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.badRequest().body("Código inválido.");
        }

        usuario.setEnabled(true);
        usuario.setVerificationCode(null);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok("Cuenta verificada con éxito.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        Usuario usuario = usuarioRepository.findByUsername(email).orElse(null);
        if (usuario == null) return ResponseEntity.ok("Si el correo existe, se envió un enlace.");

        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);
        usuario.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(usuario);

        // 👇 CAMBIO 3: APAGAMOS EL EMAIL DE RECUPERACIÓN PARA QUE NO COLAPSE
        // String link = "https://turnos-frontend-khaki.vercel.app/?token=" + token;
        // emailService.sendEmail(email, "Recuperar Contraseña", "Haz clic aquí: " + link);
        
        return ResponseEntity.ok("Función de email desactivada temporalmente.");
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("password");
        Usuario usuario = usuarioRepository.findByResetToken(token).orElse(null);
        if (usuario == null || usuario.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token inválido o expirado");
        }
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Contraseña cambiada");
    }
}