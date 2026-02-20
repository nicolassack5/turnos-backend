package com.nico.turnos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nico.turnos.entity.Rol;
import com.nico.turnos.entity.Usuario;

import org.springframework.data.domain.Page; // <-- Nueva importación
import org.springframework.data.domain.Pageable; // <-- Nueva importación
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    // 👇 ESTOS SON LOS QUE FALTABAN PARA QUE NO TIRE ERROR
    boolean existsByUsername(String username);
    Optional<Usuario> findByResetToken(String resetToken);
    
    // 👇 ESTE ES EL NUEVO QUE AGREGAMOS HOY
    Page<Usuario> findByRol(Rol rol, Pageable pageable);
}