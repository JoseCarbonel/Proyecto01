package com.javaresources.controller;

import com.javaresources.service.EmployeeService;
import com.javaresources.service.ExcelReportService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final EmployeeService empSvc;
    private final ExcelReportService excelSvc;

    public ReportController(EmployeeService empSvc, ExcelReportService excelSvc) {
        this.empSvc = empSvc; this.excelSvc = excelSvc;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel() {
        try {
            String name = "Empleados_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            byte[] data = excelSvc.generateReport(empSvc.getAll(), name);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(
                ContentDisposition.attachment().filename(name + ".xlsx").build());
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
