package com.nico.turnos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // Agregamos esto para que devuelva error 400
public class TurnoConflictException extends RuntimeException {
    public TurnoConflictException(String message) {
        super(message);
    }
}