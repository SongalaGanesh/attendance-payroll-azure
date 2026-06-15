package com.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "check_in_lat", precision = 10, scale = 8)
    private BigDecimal checkInLat;

    @Column(name = "check_in_lng", precision = 11, scale = 8)
    private BigDecimal checkInLng;

    @Column(name = "check_out_lat", precision = 10, scale = 8)
    private BigDecimal checkOutLat;

    @Column(name = "check_out_lng", precision = 11, scale = 8)
    private BigDecimal checkOutLng;

    @Column(name = "distance_meters", precision = 10, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "total_working_hours", precision = 5, scale = 2)
    private BigDecimal totalWorkingHours;

    @Column(name = "attendance_status", nullable = false, length = 20)
    private String attendanceStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
