package com.system.service;

import com.system.repository.AttendanceRepository;
import com.system.repository.EmployeeRepository;
import com.system.repository.LeaveRequestRepository;
import com.system.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    public Map<String, Object> getDashboardMetrics() {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        long totalEmployees = employeeRepository.count();
        
        long presentToday = attendanceRepository.countPresentToday(today);
        long absentToday = attendanceRepository.countAbsentToday(today);
        
        // If absent check hasn't run yet, estimate active absent today
        if (presentToday == 0 && absentToday == 0) {
            absentToday = totalEmployees;
        }

        long pendingLeaves = leaveRequestRepository.countByStatus("PENDING");
        
        BigDecimal monthlyPayroll = payrollRepository.sumNetSalaryByMonthAndYear(currentMonth, currentYear);
        if (monthlyPayroll == null) {
            monthlyPayroll = BigDecimal.ZERO;
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalEmployees", totalEmployees);
        metrics.put("presentToday", presentToday);
        metrics.put("absentToday", absentToday);
        metrics.put("pendingLeaves", pendingLeaves);
        metrics.put("monthlyPayrollAmount", monthlyPayroll);

        return metrics;
    }
}
