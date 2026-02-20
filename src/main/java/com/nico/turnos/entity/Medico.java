package com.nico.turnos.entity;

import jakarta.persistence.*;

@Entity
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String especialidad;
    private String email; // <--- ESTE ES EL QUE FALTABA

    // Constructores
    public Medico() {}

    public Medico(String nombre, String especialidad, String email) {
        this.nombre = nombre;
        this.especialidad = especialidad;
        this.email = email;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getEmail() { return email; } // <--- Getter necesario
    public void setEmail(String email) { this.email = email; } // <--- Setter necesario
}