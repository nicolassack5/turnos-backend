package com.nico.turnos.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ArchivoService {

    private final String CARPETA_SUBIDAS = "uploads/";

    public String guardarArchivo(MultipartFile archivo) throws Exception {
        // 1. Creamos la carpeta "uploads" si no existe
        Path directorio = Paths.get(CARPETA_SUBIDAS);
        if (!Files.exists(directorio)) {
            Files.createDirectories(directorio);
        }

        // 2. Le ponemos un código único (UUID) al nombre para que no se sobreescriban si dos se llaman "foto.jpg"
        String nombreUnico = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
        Path rutaArchivo = directorio.resolve(nombreUnico);

        // 3. Copiamos el archivo a la carpeta
        Files.copy(archivo.getInputStream(), rutaArchivo);

        // Retornamos solo el nombre para guardarlo en la base de datos
        return nombreUnico;
    }
}