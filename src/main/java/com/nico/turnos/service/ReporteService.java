package com.nico.turnos.service;

// 👇 IMPORTACIONES EXPLÍCITAS PARA EVITAR CONFLICTOS
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.nico.turnos.entity.Turno;
import com.nico.turnos.entity.Usuario;
import com.nico.turnos.repository.TurnoRepository;
import com.nico.turnos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteService {

    private final GeminiService geminiService;
    private final UsuarioRepository usuarioRepository;
    private final TurnoRepository turnoRepository;

    public ReporteService(GeminiService geminiService, UsuarioRepository usuarioRepository, TurnoRepository turnoRepository) {
        this.geminiService = geminiService;
        this.usuarioRepository = usuarioRepository;
        this.turnoRepository = turnoRepository;
    }

    // --- 1. GENERAR PDF LIMPIO (Sin IA y con fecha formateada) ---
    public byte[] generarReporteTurno(Turno turno) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph titulo = new Paragraph("CLÍNICA INTEGRAL - Reporte Médico", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph("\n"));

            // Formatear la fecha para sacar la 'T'
            String fechaFormateada = turno.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            document.add(new Paragraph("Paciente: " + turno.getCliente()));
            document.add(new Paragraph("Médico: Dr. " + turno.getNombreMedico()));
            document.add(new Paragraph("Fecha y Hora: " + fechaFormateada + " hs"));
            document.add(new Paragraph("Motivo de la consulta: " + turno.getDescripcion()));
            document.add(new Paragraph("\n"));

            // 👇 CORREGIDO: Usamos isAsistio()
            if (turno.isAsistio()) {
                document.add(new Paragraph("Diagnóstico y Evolución:"));
                String diagnostico = turno.getDiagnostico() != null && !turno.getDiagnostico().isEmpty() 
                                    ? turno.getDiagnostico() 
                                    : "Sin notas adicionales.";
                document.add(new Paragraph(diagnostico));
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    // --- 2. GENERAR EXCEL ---
    public ByteArrayInputStream generarExcelCompleto() throws IOException {
        String[] columnasUsuarios = {"ID", "Nombre Completo", "DNI", "Rol", "Email"};
        String[] columnasTurnos = {"ID", "Fecha y Hora", "Paciente", "Médico", "Motivo", "Asistio"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // --- PESTAÑA 1: USUARIOS ---
            Sheet sheetUsuarios = workbook.createSheet("Pacientes y Medicos");
            Row headerRow = sheetUsuarios.createRow(0);
            
            for (int i = 0; i < columnasUsuarios.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnasUsuarios[i]);
            }

            List<Usuario> usuarios = usuarioRepository.findAll();
            int rowIdx = 1;
            for (Usuario u : usuarios) {
                Row row = sheetUsuarios.createRow(rowIdx++);
                row.createCell(0).setCellValue(u.getId());
                row.createCell(1).setCellValue(u.getNombreCompleto() != null ? u.getNombreCompleto() : "");
                row.createCell(2).setCellValue(u.getDni() != null ? u.getDni() : "");
                row.createCell(3).setCellValue(u.getRol() != null ? u.getRol().name() : "");
                row.createCell(4).setCellValue(u.getUsername() != null ? u.getUsername() : "");
            }

            // --- PESTAÑA 2: TURNOS ---
            Sheet sheetTurnos = workbook.createSheet("Historial de Turnos");
            Row headerRowTurnos = sheetTurnos.createRow(0);
            for (int i = 0; i < columnasTurnos.length; i++) {
                Cell cell = headerRowTurnos.createCell(i);
                cell.setCellValue(columnasTurnos[i]);
            }

            List<Turno> turnos = turnoRepository.findAll();
            rowIdx = 1;
            for (Turno t : turnos) {
                Row row = sheetTurnos.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getId());
                row.createCell(1).setCellValue(t.getFechaHora() != null ? t.getFechaHora().toString() : "");
                row.createCell(2).setCellValue(t.getCliente() != null ? t.getCliente() : "");
                row.createCell(3).setCellValue(t.getNombreMedico() != null ? t.getNombreMedico() : "");
                row.createCell(4).setCellValue(t.getDescripcion() != null ? t.getDescripcion() : "");
                // 👇 CORREGIDO: Usamos isAsistio()
                row.createCell(5).setCellValue(t.isAsistio() ? "Sí" : "No");
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}