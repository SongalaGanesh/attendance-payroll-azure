package com.system.service;

import com.system.entity.Attendance;
import com.system.entity.Employee;
import com.system.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    /**
     * Haversine formula calculation.
     */
    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
                   
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    @Transactional
    public Attendance checkIn(Employee employee, BigDecimal latitude, BigDecimal longitude) {
        LocalDate today = LocalDate.now();
        
        // Prevent duplicate check-ins
        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today);
        if (existing.isPresent()) {
            throw new IllegalStateException("Attendance already recorded for today.");
        }

        double distance = calculateHaversineDistance(
                employee.getOfficeLatitude().doubleValue(),
                employee.getOfficeLongitude().doubleValue(),
                latitude.doubleValue(),
                longitude.doubleValue()
        );

        String status = (distance <= employee.getAllowedRadiusMeters()) ? "PRESENT" : "INVALID_LOCATION";

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .attendanceDate(today)
                .checkInTime(LocalTime.now())
                .checkInLat(latitude)
                .checkInLng(longitude)
                .distanceMeters(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP))
                .attendanceStatus(status)
                .build();

        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance checkOut(Employee employee, BigDecimal latitude, BigDecimal longitude) {
        LocalDate today = LocalDate.now();
        
        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new IllegalStateException("No check-in record found for today. Please check-in first."));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("Employee has already checked out today.");
        }

        double distance = calculateHaversineDistance(
                employee.getOfficeLatitude().doubleValue(),
                employee.getOfficeLongitude().doubleValue(),
                latitude.doubleValue(),
                longitude.doubleValue()
        );

        LocalTime checkOutTime = LocalTime.now();
        attendance.setCheckOutTime(checkOutTime);
        attendance.setCheckOutLat(latitude);
        attendance.setCheckOutLng(longitude);
        attendance.setDistanceMeters(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP));

        // Calculate working hours
        if (attendance.getCheckInTime() != null) {
            Duration duration = Duration.between(attendance.getCheckInTime(), checkOutTime);
            double hours = duration.toMinutes() / 60.0;
            attendance.setTotalWorkingHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));
        }

        return attendanceRepository.save(attendance);
    }
}
