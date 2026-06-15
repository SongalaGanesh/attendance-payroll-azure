package com.system.controller;

import com.system.entity.Attendance;
import com.system.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/attendance")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_ADMIN')")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @GetMapping
    public ResponseEntity<Page<Attendance>> getDailyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Attendance> attendancePage = attendanceRepository.findByAttendanceDate(queryDate, pageable);
        return ResponseEntity.ok(attendancePage);
    }
}
