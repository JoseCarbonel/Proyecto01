package com.javaresources.service;

import com.javaresources.model.Employee;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelReportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelReportService.class);
    private static final String[] HEADERS = {"ID","Nombre","Email","Departamento","Rol","Salario (S/)","Contratación","Skills"};

    @Value("${app.output.dir:output/reports}")
    private String outputDir;

    public byte[] generateReport(Collection<Employee> employees, String reportName) throws IOException {
        log.info("Generando Excel: {} empleados", employees.size());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Empleados");
            sheet.setDefaultColumnWidth(18);

            // Título
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(26);
            Cell tc = titleRow.createCell(0);
            tc.setCellValue("Reporte de Empleados — " + reportName);
            tc.setCellStyle(titleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            // Encabezados
            Row hRow = sheet.createRow(1);
            hRow.setHeightInPoints(20);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle(wb));
            }

            int rowNum = 2; double total = 0;
            for (Employee e : employees) {
                Row row = sheet.createRow(rowNum);
                XSSFCellStyle rs = (rowNum % 2 == 0) ? dataStyle(wb) : altStyle(wb);
                row.createCell(0).setCellValue(e.getId());
                row.createCell(1).setCellValue(safe(e.getName()));
                row.createCell(2).setCellValue(safe(e.getEmail()));
                row.createCell(3).setCellValue(safe(e.getDepartment()));
                row.createCell(4).setCellValue(safe(e.getRole()));
                Cell sc = row.createCell(5); sc.setCellValue(e.getSalary()); sc.setCellStyle(salaryStyle(wb));
                row.createCell(6).setCellValue(e.getHireDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                row.createCell(7).setCellValue(String.join(", ", e.getSkills()));
                for (int c = 0; c < HEADERS.length; c++) {
                    if (c != 5 && row.getCell(c) != null) row.getCell(c).setCellStyle(rs);
                }
                total += e.getSalary(); rowNum++;
            }

            Row totRow = sheet.createRow(rowNum + 1);
            Cell lbl = totRow.createCell(4); lbl.setCellValue("TOTAL:"); lbl.setCellStyle(headerStyle(wb));
            Cell tot = totRow.createCell(5); tot.setCellValue(total); tot.setCellStyle(salaryStyle(wb));

            // Hoja resumen
            createSummarySheet(wb.createSheet("Resumen"), employees, wb);
            sheet.createFreezePane(0, 2);
            sheet.setAutoFilter(new CellRangeAddress(1, rowNum - 1, 0, HEADERS.length - 1));
            wb.write(baos);
        }
        log.info("Excel generado en memoria: {} bytes", baos.size());
        return baos.toByteArray();
    }

    private String safe(String v) {
        if (StringUtils.isBlank(v)) return "";
        String s = v.trim();
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) {
            log.warn("SEGURIDAD: Excel Injection prevenida en celda: '{}'", s);
            return "'" + s;
        }
        return s;
    }

    private void createSummarySheet(XSSFSheet sheet, Collection<Employee> employees, XSSFWorkbook wb) {
        Map<String,Long> cnt = new LinkedHashMap<>();
        Map<String,Double> sal = new LinkedHashMap<>();
        for (Employee e : employees) {
            cnt.merge(e.getDepartment(), 1L, Long::sum);
            sal.merge(e.getDepartment(), e.getSalary(), Double::sum);
        }
        XSSFCellStyle hs = headerStyle(wb);
        Row h = sheet.createRow(0);
        String[] cols = {"Departamento","Empleados","Planilla (S/)","Promedio (S/)"};
        for (int i = 0; i < cols.length; i++) { h.createCell(i).setCellValue(cols[i]); h.getCell(i).setCellStyle(hs); }
        int r = 1;
        for (String d : cnt.keySet()) {
            long c = cnt.get(d); double t = sal.get(d);
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(d); row.createCell(1).setCellValue(c);
            row.createCell(2).setCellValue(t); row.createCell(3).setCellValue(c > 0 ? t / c : 0);
        }
        sheet.autoSizeColumn(0);
    }

    private XSSFCellStyle titleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle(); XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short)14); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)31,(byte)73,(byte)125}));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER); s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }
    private XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle(); XSSFFont f = wb.createFont();
        f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex()); s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)68,(byte)114,(byte)196}));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER); return s;
    }
    private XSSFCellStyle dataStyle(XSSFWorkbook wb)  { return wb.createCellStyle(); }
    private XSSFCellStyle altStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)217,(byte)226,(byte)243}));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND); return s;
    }
    private XSSFCellStyle salaryStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT); return s;
    }
}
