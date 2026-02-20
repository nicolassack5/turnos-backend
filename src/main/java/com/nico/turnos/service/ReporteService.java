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

    // --- 1. GENERAR PDF CON IA ---
    public byte[] generarReporteTurno(Turno turno) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph titulo = new Paragraph("CLÍNICA INTEGRAL - Comprobante", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Paciente: " + turno.getCliente()));
            document.add(new Paragraph("Médico: " + turno.getNombreMedico()));
            document.add(new Paragraph("Fecha: " + turno.getFechaHora().toString()));
            document.add(new Paragraph("Descripción: " + turno.getDescripcion()));

            try {
                String consejo = geminiService.preguntarAGemini("Dame un consejo corto de salud para un paciente que consultó por: " + turno.getDescripcion());
                document.add(new Paragraph("\nNota del Asistente Virtual:"));
                document.add(new Paragraph(consejo));
            } catch (Exception e) {
                document.add(new Paragraph("\nAsistente temporalmente no disponible."));
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
        String[] columnasTurnos = {"ID", "Fecha y Hora", "Paciente", "Médico", "Motivo", "Asistió"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // --- PESTAÑA 1: USUARIOS ---
            Sheet sheetUsuarios = workbook.createSheet("Pacientes y Médicos");
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
                row.createCell(5).setCellValue(t.isAsistio() ? "Sí" : "No");
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}