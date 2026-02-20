package com.nico.turnos.config;

import com.nico.turnos.entity.Rol;
import com.nico.turnos.entity.Usuario;
import com.nico.turnos.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CargadorDatos {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verificamos si ya existe el admin para no duplicarlo
            if (!usuarioRepository.existsByUsername("admin@admin.com")) {
                Usuario admin = new Usuario();
                admin.setUsername("admin@admin.com");
                admin.setPassword(passwordEncoder.encode("123456")); 
                admin.setNombreCompleto("Administrador del Sistema");
                admin.setRol(Rol.ADMIN);
                
                // --- LA LÍNEA CRÍTICA ---
                // Marcamos al admin como habilitado para que no necesite verificar email
                admin.setEnabled(true); 
                
                usuarioRepository.save(admin);
                
                System.out.println("--------------------------------------");
                System.out.println("✅ USUARIO ADMIN CREADO Y ACTIVADO");
                System.out.println("📧 Email: admin@admin.com");
                System.out.println("🔑 Clave: 123456");
                System.out.println("--------------------------------------");
            }
        };
    }
}