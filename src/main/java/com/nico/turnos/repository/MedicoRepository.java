package com.nico.turnos.repository;
import com.nico.turnos.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MedicoRepository extends JpaRepository<Medico, Long> {}