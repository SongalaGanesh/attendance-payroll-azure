package com.system.controller;

import com.system.entity.LeaveRequest;
import com.system.entity.User;
import com.system.repository.UserRepository;
import com.system.security.UserDetailsImpl;
import com.system.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR_ADMIN')")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<LeaveRequest>> getLeaves(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<LeaveRequest> leaves = leaveService.getAllLeaves(status, pageable);
        return ResponseEntity.ok(leaves);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<LeaveRequest> approveLeave(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        User admin = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found."));
        String remarks = body.getOrDefault("remarks", "Approved by Admin");
        LeaveRequest approved = leaveService.approveLeave(id, admin, remarks);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<LeaveRequest> rejectLeave(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        User admin = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found."));
        String remarks = body.getOrDefault("remarks", "Rejected by Admin");
        LeaveRequest rejected = leaveService.rejectLeave(id, admin, remarks);
        return ResponseEntity.ok(rejected);
    }
}
