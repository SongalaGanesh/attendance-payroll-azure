package com.system.service;

import com.system.entity.Employee;
import com.system.entity.Payroll;
import com.system.repository.AttendanceRepository;
import com.system.repository.EmployeeRepository;
import com.system.repository.LeaveRequestRepository;
import com.system.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    public Page<Payroll> getPayrollHistory(Integer month, Integer year, Pageable pageable) {
        if (month != null && year != null) {
            return payrollRepository.findByMonthAndYear(month, year, pageable);
        }
        return payrollRepository.findAll(pageable);
    }

    public Page<Payroll> getEmployeePayrolls(Long employeeId, Pageable pageable) {
        return payrollRepository.findByEmployeeId(employeeId, pageable);
    }

    @Transactional
    public Payroll processPayroll(Long employeeId, int month, int year, int workingDays) {
        if (workingDays <= 0) {
            throw new IllegalArgumentException("Working days must be greater than zero.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));

        // Check if payroll already exists for the month
        Optional<Payroll> existing = payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year);
        if (existing.isPresent()) {
            throw new IllegalStateException("Payroll already processed for this employee, month, and year.");
        }

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // Count present days
        List<com.system.entity.Attendance> attendanceList = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetween(employeeId, start, end);
        int presentDays = (int) attendanceList.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getAttendanceStatus()))
                .count();

        // Count approved leave days
        Integer approvedLeaves = leaveRequestRepository.countApprovedLeaveDaysInPeriod(employeeId, start, end);
        if (approvedLeaves == null) {
            approvedLeaves = 0;
        }

        // Calculate absent days
        int eligibleDays = presentDays + approvedLeaves;
        int absentDays = Math.max(0, workingDays - eligibleDays);

        // Apply salary formula: Salary = MonthlySalary * (PresentDays + ApprovedLeaveDays) / WorkingDays
        BigDecimal monthlySalary = employee.getMonthlySalary();
        BigDecimal netSalary = BigDecimal.ZERO;
        if (workingDays > 0) {
            BigDecimal factor = BigDecimal.valueOf(eligibleDays)
                    .divide(BigDecimal.valueOf(workingDays), 4, RoundingMode.HALF_UP);
            netSalary = monthlySalary.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }

        Payroll payroll = Payroll.builder()
                .employee(employee)
                .month(month)
                .year(year)
                .workingDays(workingDays)
                .presentDays(presentDays)
                .approvedLeaveDays(approvedLeaves)
                .absentDays(absentDays)
                .grossSalary(monthlySalary)
                .netSalary(netSalary)
                .status("PROCESSED")
                .processedAt(LocalDateTime.now())
                .build();

        return payrollRepository.save(payroll);
    }
}
