package com.saia.business;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.saia.model.FiltrosReporte;
import com.saia.model.FormatoDescarga;
import com.saia.model.TipoReporte;

/**
 * Servicio de exportación de reportes.
 * Soporta Excel (.xlsx via Apache POI), PDF (.pdf via OpenPDF), CSV (nativo).
 */
public class ReporteExportService {

    private static final DateTimeFormatter FMT_FILE =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Genera el archivo y devuelve la ruta absoluta del archivo creado.
     * @param dir       Directorio destino (normalmente elegido por el usuario)
     * @param tipo      Tipo de reporte
     * @param filtros   Filtros aplicados
     * @param formato   Formato de exportación
     * @param columnas  Cabeceras de columna
     * @param filas     Datos (lista de mapas ordenados)
     */
    public File exportar(File dir, TipoReporte tipo, FiltrosReporte filtros,
                         FormatoDescarga formato, List<String> columnas,
                         List<Map<String, Object>> filas) throws IOException {

        String nombre = "SAIA_" + tipo.name() + "_" + LocalDateTime.now().format(FMT_FILE)
                + formato.extension;
        File destino = new File(dir, nombre);

        switch (formato) {
            case EXCEL -> exportarExcel(destino, tipo, filtros, columnas, filas);
            case PDF   -> exportarPDF  (destino, tipo, filtros, columnas, filas);
            case CSV   -> exportarCSV  (destino,               columnas, filas);
        }
        return destino;
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    private void exportarExcel(File f, TipoReporte tipo,
                                FiltrosReporte filtros,
                                List<String> columnas,
                                List<Map<String, Object>> filas) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(tipo.titulo);

            // Estilo encabezado
            CellStyle hdrStyle = wb.createCellStyle();
            hdrStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            hdrStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hdrStyle.setAlignment(HorizontalAlignment.CENTER);
            hdrStyle.setBorderBottom(BorderStyle.THIN);
            Font hdrFont = wb.createFont();
            hdrFont.setBold(true);
            hdrFont.setColor(IndexedColors.WHITE.getIndex());
            hdrStyle.setFont(hdrFont);

            // Estilo par/impar
            CellStyle evenStyle = wb.createCellStyle();
            evenStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Fila 0 — título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SAIA — " + tipo.titulo);
            CellStyle tStyle = wb.createCellStyle();
            Font tFont = wb.createFont();
            tFont.setBold(true); tFont.setFontHeightInPoints((short) 14);
            tStyle.setFont(tFont);
            titleCell.setCellStyle(tStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                    0, 0, 0, columnas.size() - 1));

            // Fila 1 — metadata de filtros
            Row metaRow = sheet.createRow(1);
            String periodo = "";
            if (filtros.getFechaInicio() != null || filtros.getFechaFin() != null) {
                periodo = "Período: " +
                    (filtros.getFechaInicio() != null ? filtros.getFechaInicio().toString() : "—") +
                    " al " +
                    (filtros.getFechaFin() != null ? filtros.getFechaFin().toString() : "—");
            }
            String generated = "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Cell metaCell = metaRow.createCell(0);
            metaCell.setCellValue(periodo + "    " + generated +
                    "    Total: " + filas.size() + " registros");
            CellStyle metaStyle = wb.createCellStyle();
            Font metaFont = wb.createFont();
            metaFont.setItalic(true); metaFont.setFontHeightInPoints((short)10);
            metaFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            metaStyle.setFont(metaFont);
            metaCell.setCellStyle(metaStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                    1, 1, 0, columnas.size() - 1));

            // Fila 2 — cabecera de columnas
            Row hdr = sheet.createRow(2);
            for (int c = 0; c < columnas.size(); c++) {
                Cell cell = hdr.createCell(c);
                cell.setCellValue(columnas.get(c));
                cell.setCellStyle(hdrStyle);
            }

            // Datos desde fila 3
            int rowIdx = 3;
            for (Map<String, Object> fila : filas) {
                Row row = sheet.createRow(rowIdx);
                CellStyle rowStyle = (rowIdx % 2 == 0) ? evenStyle : null;
                int c = 0;
                for (Object val : fila.values()) {
                    Cell cell = row.createCell(c++);
                    cell.setCellValue(val != null ? val.toString() : "");
                    if (rowStyle != null) cell.setCellStyle(rowStyle);
                }
                rowIdx++;
            }

            // Auto-ancho
            for (int c = 0; c < columnas.size(); c++) {
                sheet.autoSizeColumn(c);
                sheet.setColumnWidth(c, Math.min(sheet.getColumnWidth(c) + 512, 15000));
            }

            try (FileOutputStream fos = new FileOutputStream(f)) { wb.write(fos); }
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    private void exportarPDF(File f, TipoReporte tipo,
                              FiltrosReporte filtros,
                              List<String> columnas,
                              List<Map<String, Object>> filas) throws IOException {
        Document doc = null;
        FileOutputStream fos = null;
        try {
            doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 50);
            fos = new FileOutputStream(f);
            PdfWriter.getInstance(doc, fos);
            doc.open();

            // === ENCABEZADO DEL INFORME ===
            
            // Logo/Título SAIA con borde superior
            com.lowagie.text.Font logoFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 20,
                    com.lowagie.text.Font.BOLD,
                    new java.awt.Color(0x39A900));
            Paragraph logo = new Paragraph("SAIA", logoFont);
            logo.setAlignment(Element.ALIGN_LEFT);
            logo.setSpacingAfter(2f);
            doc.add(logo);

            // Subtítulo del sistema
            com.lowagie.text.Font sysFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 8,
                    com.lowagie.text.Font.NORMAL,
                    new java.awt.Color(0x666666));
            Paragraph sys = new Paragraph("Sistema de Apoyo a la Información del Aprendiz", sysFont);
            sys.setAlignment(Element.ALIGN_LEFT);
            sys.setSpacingAfter(15f);
            doc.add(sys);

            // Línea separadora
            PdfPTable linea1 = new PdfPTable(1);
            linea1.setWidthPercentage(100f);
            linea1.setSpacingAfter(15f);
            PdfPCell lineaCell = new PdfPCell();
            lineaCell.setBorder(0);
            lineaCell.setBorderWidthBottom(2f);
            lineaCell.setBorderColorBottom(new java.awt.Color(0x39A900));
            lineaCell.setPadding(0);
            linea1.addCell(lineaCell);
            doc.add(linea1);

            // Título del reporte
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 16,
                    com.lowagie.text.Font.BOLD,
                    new java.awt.Color(0x1A3A5C));
            Paragraph titulo = new Paragraph(tipo.titulo.toUpperCase(), titleFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(8f);
            doc.add(titulo);

            // Información del reporte en tabla de 2 columnas
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(60f);
            infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            infoTable.setSpacingAfter(15f);
            
            com.lowagie.text.Font infoLabelFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 9,
                    com.lowagie.text.Font.BOLD,
                    new java.awt.Color(0x333333));
            com.lowagie.text.Font infoValueFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 9,
                    com.lowagie.text.Font.NORMAL,
                    new java.awt.Color(0x666666));

            // Fecha de generación
            PdfPCell labelCell = new PdfPCell(new Phrase("Fecha de generación:", infoLabelFont));
            labelCell.setBorder(0);
            labelCell.setPadding(3f);
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            infoTable.addCell(labelCell);

            PdfPCell valueCell = new PdfPCell(new Phrase(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), 
                infoValueFont));
            valueCell.setBorder(0);
            valueCell.setPadding(3f);
            infoTable.addCell(valueCell);

            // Período (si aplica)
            if (filtros.getFechaInicio() != null || filtros.getFechaFin() != null) {
                labelCell = new PdfPCell(new Phrase("Período:", infoLabelFont));
                labelCell.setBorder(0);
                labelCell.setPadding(3f);
                labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                infoTable.addCell(labelCell);

                String periodoStr = (filtros.getFechaInicio() != null ? 
                    filtros.getFechaInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—") +
                    " al " +
                    (filtros.getFechaFin() != null ? 
                    filtros.getFechaFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—");
                
                valueCell = new PdfPCell(new Phrase(periodoStr, infoValueFont));
                valueCell.setBorder(0);
                valueCell.setPadding(3f);
                infoTable.addCell(valueCell);
            }

            // Total de registros
            labelCell = new PdfPCell(new Phrase("Total de registros:", infoLabelFont));
            labelCell.setBorder(0);
            labelCell.setPadding(3f);
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            infoTable.addCell(labelCell);

            valueCell = new PdfPCell(new Phrase(String.valueOf(filas.size()), infoValueFont));
            valueCell.setBorder(0);
            valueCell.setPadding(3f);
            infoTable.addCell(valueCell);

            doc.add(infoTable);

            // === TABLA DE DATOS ===

            if (!columnas.isEmpty() && !filas.isEmpty()) {
                PdfPTable table = new PdfPTable(columnas.size());
                table.setWidthPercentage(100f);
                table.setSpacingBefore(5f);

                // Cabecera con estilo mejorado
                com.lowagie.text.Font hFont = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 8,
                        com.lowagie.text.Font.BOLD,
                        java.awt.Color.WHITE);
                java.awt.Color hdrColor = new java.awt.Color(0x39A900); // Verde SENA
                
                for (String col : columnas) {
                    PdfPCell cell = new PdfPCell(new Phrase(col.toUpperCase(), hFont));
                    cell.setBackgroundColor(hdrColor);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(6f);
                    cell.setBorderWidth(1f);
                    cell.setBorderColor(java.awt.Color.WHITE);
                    table.addCell(cell);
                }

                // Filas de datos con alternancia de colores
                com.lowagie.text.Font dFont = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 7,
                        com.lowagie.text.Font.NORMAL,
                        new java.awt.Color(0x333333));
                java.awt.Color colorPar = java.awt.Color.WHITE;
                java.awt.Color colorImpar = new java.awt.Color(0xF8F9FA);
                
                int idx = 0;
                for (Map<String, Object> fila : filas) {
                    java.awt.Color bg = (idx++ % 2 == 0) ? colorPar : colorImpar;
                    for (Object val : fila.values()) {
                        String texto = val != null ? val.toString() : "";
                        // Limitar longitud de texto para evitar celdas muy grandes
                        if (texto.length() > 100) {
                            texto = texto.substring(0, 97) + "...";
                        }
                        PdfPCell cell = new PdfPCell(new Phrase(texto, dFont));
                        cell.setBackgroundColor(bg);
                        cell.setPadding(4f);
                        cell.setPaddingLeft(6f);
                        cell.setPaddingRight(6f);
                        cell.setBorderWidth(0.5f);
                        cell.setBorderColor(new java.awt.Color(0xDEE2E6));
                        table.addCell(cell);
                    }
                }
                doc.add(table);
            } else {
                com.lowagie.text.Font emptyFont = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 11,
                        com.lowagie.text.Font.ITALIC,
                        new java.awt.Color(0x999999));
                Paragraph empty = new Paragraph("No se encontraron datos para los filtros seleccionados.", emptyFont);
                empty.setAlignment(Element.ALIGN_CENTER);
                empty.setSpacingBefore(30f);
                doc.add(empty);
            }

            // === PIE DE PÁGINA ===
            
            // Línea separadora inferior
            PdfPTable linea2 = new PdfPTable(1);
            linea2.setWidthPercentage(100f);
            linea2.setSpacingBefore(20f);
            linea2.setSpacingAfter(8f);
            lineaCell = new PdfPCell();
            lineaCell.setBorder(0);
            lineaCell.setBorderWidthTop(1f);
            lineaCell.setBorderColorTop(new java.awt.Color(0xDEE2E6));
            lineaCell.setPadding(0);
            linea2.addCell(lineaCell);
            doc.add(linea2);

            // Información del pie
            com.lowagie.text.Font pieFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 7,
                    com.lowagie.text.Font.NORMAL,
                    new java.awt.Color(0x999999));
            
            Paragraph pie = new Paragraph(
                "SAIA - Sistema de Apoyo a la Información del Aprendiz  |  " +
                "Centro de Biotecnología Agropecuaria - SENA  |  " +
                "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                pieFont);
            pie.setAlignment(Element.ALIGN_CENTER);
            doc.add(pie);

        } catch (DocumentException e) {
            throw new IOException("Error generando PDF: " + e.getMessage(), e);
        } finally {
            if (doc != null && doc.isOpen()) {
                doc.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                    // Ya cerrado o error al cerrar
                }
            }
        }
    }

    // ── CSV ───────────────────────────────────────────────────────────────────

    private void exportarCSV(File f,
                              List<String> columnas,
                              List<Map<String, Object>> filas) throws IOException {
        try (FileWriter fw = new FileWriter(f, java.nio.charset.StandardCharsets.UTF_8)) {
            // BOM para compatibilidad con Excel
            fw.write('\uFEFF');
            // Cabecera
            fw.write(String.join(",", columnas.stream()
                    .map(c -> '"' + c + '"').toList()) + "\n");
            // Datos
            for (Map<String, Object> fila : filas) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Object val : fila.values()) {
                    if (!first) sb.append(',');
                    String v = val != null ? val.toString().replace("\"", "\"\"") : "";
                    sb.append('"').append(v).append('"');
                    first = false;
                }
                fw.write(sb + "\n");
            }
        }
    }
}
