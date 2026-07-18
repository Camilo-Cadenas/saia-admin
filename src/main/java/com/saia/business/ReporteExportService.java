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
import com.saia.model.FormatoDescarga;
import com.saia.model.FiltrosReporte;
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
            case EXCEL -> exportarExcel(destino, tipo, columnas, filas);
            case PDF   -> exportarPDF  (destino, tipo, columnas, filas);
            case CSV   -> exportarCSV  (destino,        columnas, filas);
        }
        return destino;
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    private void exportarExcel(File f, TipoReporte tipo,
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

            // Fila de título del reporte
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SAIA – " + tipo.titulo);
            CellStyle tStyle = wb.createCellStyle();
            Font tFont = wb.createFont();
            tFont.setBold(true);
            tFont.setFontHeightInPoints((short) 14);
            tStyle.setFont(tFont);
            titleCell.setCellStyle(tStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                    0, 0, 0, columnas.size() - 1));

            // Cabecera de columnas
            Row hdr = sheet.createRow(1);
            for (int c = 0; c < columnas.size(); c++) {
                Cell cell = hdr.createCell(c);
                cell.setCellValue(columnas.get(c));
                cell.setCellStyle(hdrStyle);
            }

            // Datos
            int rowIdx = 2;
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

            // Auto-ancho de columnas
            for (int c = 0; c < columnas.size(); c++) {
                sheet.autoSizeColumn(c);
                int curWidth = sheet.getColumnWidth(c);
                sheet.setColumnWidth(c, Math.min(curWidth + 512, 15000));
            }

            try (FileOutputStream fos = new FileOutputStream(f)) {
                wb.write(fos);
            }
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    private void exportarPDF(File f, TipoReporte tipo,
                              List<String> columnas,
                              List<Map<String, Object>> filas) throws IOException {
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(f));
            doc.open();

            // Título
            com.lowagie.text.Font tFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 16,
                    com.lowagie.text.Font.BOLD,
                    new java.awt.Color(0x1A3A5C));
            Paragraph titulo = new Paragraph("SAIA – " + tipo.titulo, tFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(14f);
            doc.add(titulo);

            // Subtítulo con fecha
            com.lowagie.text.Font subFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 9,
                    com.lowagie.text.Font.NORMAL,
                    java.awt.Color.GRAY);
            Paragraph sub = new Paragraph(
                "Generado el " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(10f);
            doc.add(sub);

            // Tabla
            if (!columnas.isEmpty()) {
                PdfPTable table = new PdfPTable(columnas.size());
                table.setWidthPercentage(100f);

                // Cabecera
                com.lowagie.text.Font hFont = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 8,
                        com.lowagie.text.Font.BOLD,
                        java.awt.Color.WHITE);
                java.awt.Color hdrColor = new java.awt.Color(0x1A3A5C);
                for (String col : columnas) {
                    PdfPCell cell = new PdfPCell(new Phrase(col, hFont));
                    cell.setBackgroundColor(hdrColor);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(5f);
                    table.addCell(cell);
                }

                // Filas
                com.lowagie.text.Font dFont = new com.lowagie.text.Font(
                        com.lowagie.text.Font.HELVETICA, 7);
                java.awt.Color even = new java.awt.Color(0xEEF2FF);
                int idx = 0;
                for (Map<String, Object> fila : filas) {
                    java.awt.Color bg = (idx++ % 2 == 0) ? java.awt.Color.WHITE : even;
                    for (Object val : fila.values()) {
                        PdfPCell cell = new PdfPCell(
                                new Phrase(val != null ? val.toString() : "", dFont));
                        cell.setBackgroundColor(bg);
                        cell.setPadding(4f);
                        table.addCell(cell);
                    }
                }
                doc.add(table);
            } else {
                doc.add(new Paragraph("Sin datos para los filtros seleccionados."));
            }

            // Pie de página
            Paragraph pie = new Paragraph(
                "Total de registros: " + filas.size() + "  |  SAIA Admin v1.0", subFont);
            pie.setAlignment(Element.ALIGN_RIGHT);
            pie.setSpacingBefore(12f);
            doc.add(pie);

        } catch (DocumentException e) {
            throw new IOException("Error generando PDF: " + e.getMessage(), e);
        } finally {
            doc.close();
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
