package com.system.service;

import com.system.entity.Employee;
import com.system.entity.LeaveHistory;
import com.system.entity.LeaveRequest;
import com.system.entity.User;
import com.system.repository.LeaveHistoryRepository;
import com.system.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveHistoryRepository leaveHistoryRepository;

    @Autowired
    @Lazy
    private WhatsAppService whatsAppService;

    public Page<LeaveRequest> getAllLeaves(String status, Pageable pageable) {
        if (status != null && !status.trim().isEmpty()) {
            return leaveRequestRepository.findByStatus(status, pageable);
        }
        return leaveRequestRepository.findAll(pageable);
    }

    public Page<LeaveRequest> getEmployeeLeaves(Long employeeId, Pageable pageable) {
        return leaveRequestRepository.findByEmployeeId(employeeId, pageable);
    }

    @Transactional
    public LeaveRequest applyLeave(Employee employee, String leaveType, LocalDate fromDate, LocalDate toDate, String reason) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date.");
        }
        if (fromDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Leave cannot be applied for past dates.");
        }

        int totalDays = (int) ChronoUnit.DAYS.between(fromDate, toDate) + 1;

        LeaveRequest request = LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType.toUpperCase())
                .fromDate(fromDate)
                .toDate(toDate)
                .totalDays(totalDays)
                .reason(reason)
                .status("PENDING")
                .build();

        request = leaveRequestRepository.save(request);

        // Record initial history
        LeaveHistory history = LeaveHistory.builder()
                .leaveRequest(request)
                .action("APPLIED")
                .remarks("Applied via WhatsApp")
                .build();
        leaveHistoryRepository.save(history);

        return request;
    }

    @Transactional
    public LeaveRequest approveLeave(Long id, User admin, String remarks) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found."));

        if (!request.getStatus().equals("PENDING")) {
            throw new IllegalStateException("Only pending leaves can be approved.");
        }

        request.setStatus("APPROVED");
        request.setApprovedBy(admin);
        request.setApprovedAt(LocalDateTime.now());
        request = leaveRequestRepository.save(request);

        // Save history
        LeaveHistory history = LeaveHistory.builder()
                .leaveRequest(request)
                .action("APPROVED")
                .actionedBy(admin)
                .remarks(remarks)
                .build();
        leaveHistoryRepository.save(history);

        // Send WhatsApp update to employee
        String notification = String.format("Dear %s, your leave request for %s to %s has been APPROVED. Remarks: %s",
                request.getEmployee().getFirstName(), request.getFromDate(), request.getToDate(), remarks);
        whatsAppService.sendWhatsAppMessage(request.getEmployee().getWhatsappNumber(), notification);

        return request;
    }

    @Transactional
    public LeaveRequest rejectLeave(Long id, User admin, String remarks) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found."));

        if (!request.getStatus().equals("PENDING")) {
            throw new IllegalStateException("Only pending leaves can be rejected.");
        }

        request.setStatus("REJECTED");
        request.setApprovedBy(admin);
        request.setApprovedAt(LocalDateTime.now());
        request = leaveRequestRepository.save(request);

        // Save history
        LeaveHistory history = LeaveHistory.builder()
                .leaveRequest(request)
                .action("REJECTED")
                .actionedBy(admin)
                .remarks(remarks)
                .build();
        leaveHistoryRepository.save(history);

        // Send WhatsApp update to employee
        String notification = String.format("Dear %s, your leave request for %s to %s has been REJECTED. Remarks: %s",
                request.getEmployee().getFirstName(), request.getFromDate(), request.getToDate(), remarks);
        whatsAppService.sendWhatsAppMessage(request.getEmployee().getWhatsappNumber(), notification);

        return request;
    }
}
