package com.nico.turnos.controller;

import com.nico.turnos.dto.PasswordUpdateRequest;
import com.nico.turnos.entity.Rol;
import com.nico.turnos.entity.Usuario;
import com.nico.turnos.repository.UsuarioRepository;
import com.nico.turnos.service.ArchivoService; // <-- NUEVA IMPORTACIÓN
import com.nico.turnos.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/usuario")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ArchivoService archivoService; // <-- NUEVA DEPENDENCIA

    // 👇 Inyectamos el ArchivoService en el constructor
    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, EmailService emailService, ArchivoService archivoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.archivoService = archivoService;
    }

    @GetMapping("/todos")
    public ResponseEntity<List<Usuario>> listarTodos() { 
        return ResponseEntity.ok(usuarioRepository.findAll()); 
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> editarUsuarioAdmin(@PathVariable Long id, @RequestBody Usuario datos) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuario.setNombreCompleto(datos.getNombreCompleto());
        usuario.setDni(datos.getDni());
        usuario.setTelefono(datos.getTelefono());
        usuario.setUsername(datos.getUsername());
        usuario.setRol(datos.getRol());
        usuario.setEspecialidad(datos.getEspecialidad());
        
        // FORZAR HABILITACIÓN PARA MÉDICOS Y ADMINS
        if (datos.getRol() == Rol.MEDICO || datos.getRol() == Rol.ADMIN) {
            usuario.setEnabled(true);
        }

        return ResponseEntity.ok(usuarioRepository.save(usuario));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) { 
        usuarioRepository.deleteById(id); 
        return ResponseEntity.ok("Eliminado"); 
    }

    @GetMapping("/perfil")
    public ResponseEntity<Usuario> obtenerPerfil() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(usuario);
    }
    
    @PutMapping("/perfil")
    public ResponseEntity<Usuario> actualizarPerfil(@RequestBody Usuario datos) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        usuario.setNombreCompleto(datos.getNombreCompleto());
        usuario.setDni(datos.getDni());
        usuario.setTelefono(datos.getTelefono());
        return ResponseEntity.ok(usuarioRepository.save(usuario));
    }

    @PostMapping("/perfil/password/solicitar-codigo")
    public ResponseEntity<?> solicitarCodigoPassword() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        String codigo = String.format("%06d", new Random().nextInt(999999));
        usuario.setVerificationCode(codigo);
        usuarioRepository.save(usuario);
        emailService.sendEmail(usuario.getUsername(), "Código Seguridad", "Tu código es: " + codigo);
        return ResponseEntity.ok("Código enviado.");
    }

    @PutMapping("/perfil/password/confirmar")
    public ResponseEntity<?> cambiarPasswordConCodigo(@RequestBody PasswordUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        if (!passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword())) {
            return ResponseEntity.badRequest().body("Contraseña actual incorrecta.");
        }
        if (usuario.getVerificationCode() == null || !usuario.getVerificationCode().equals(request.getVerificationCode())) {
            return ResponseEntity.badRequest().body("Código incorrecto.");
        }
        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usuario.setVerificationCode(null);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Contraseña actualizada.");
    }

    // 👇 NUEVO ENDPOINT DE PAGINACIÓN CORREGIDO
    @GetMapping("/paginados")
    public org.springframework.http.ResponseEntity<org.springframework.data.domain.Page<Usuario>> getUsuariosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) Rol rol) { // <-- Ahora usa tu Enum Rol
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        
        org.springframework.data.domain.Page<Usuario> usuariosPage;
        if (rol != null) {
            usuariosPage = usuarioRepository.findByRol(rol, pageable);
        } else {
            usuariosPage = usuarioRepository.findAll(pageable);
        }
        
        return org.springframework.http.ResponseEntity.ok(usuariosPage);
    }

    // 👇 NUEVO ENDPOINT PARA RECIBIR LA FOTO DESDE REACT
    @PostMapping("/perfil/foto")
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> subirFotoPerfil(
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo,
            java.security.Principal principal) {
        try {
            // 1. Buscamos quién es el usuario logueado
            String username = principal.getName();
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // 2. Guardamos el archivo físico en la compu
            String nombreArchivo = archivoService.guardarArchivo(archivo);

            // 3. Le guardamos la URL al usuario (ej: http://localhost:8080/uploads/mifoto.jpg)
            String urlFoto = "http://localhost:8080/uploads/" + nombreArchivo;
            usuario.setFotoPerfil(urlFoto);
            usuarioRepository.save(usuario);

            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "mensaje", "Foto actualizada con éxito",
                    "fotoPerfil", urlFoto
            ));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", "Error al subir la foto: " + e.getMessage()
            ));
        }
    }
}