package com.system.controller;

import com.system.entity.Payroll;
import com.system.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/payroll")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_ADMIN')")
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @GetMapping
    public ResponseEntity<Page<Payroll>> getPayrollList(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Payroll> payrollPage = payrollService.getPayrollHistory(month, year, pageable);
        return ResponseEntity.ok(payrollPage);
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayroll(@RequestBody Map<String, Object> body) {
        try {
            Long employeeId = Long.valueOf(body.get("employeeId").toString());
            int month = Integer.parseInt(body.get("month").toString());
            int year = Integer.parseInt(body.get("year").toString());
            int workingDays = Integer.parseInt(body.get("workingDays").toString());

            Payroll processed = payrollService.processPayroll(employeeId, month, year, workingDays);
            return ResponseEntity.ok(processed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
