package com.system.repository;

import com.system.entity.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    
    Page<LeaveRequest> findByStatus(String status, Pageable pageable);
    
    Long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(l.totalDays), 0) FROM LeaveRequest l WHERE " +
           "l.employee.id = :employeeId AND l.status = 'APPROVED' AND " +
           "((l.fromDate >= :start AND l.fromDate <= :end) OR (l.toDate >= :start AND l.toDate <= :end))")
    Integer countApprovedLeaveDaysInPeriod(@Param("employeeId") Long employeeId, 
                                          @Param("start") LocalDate start, 
                                          @Param("end") LocalDate end);

    @Query("SELECT l FROM LeaveRequest l WHERE l.employee.id = :employeeId AND l.status = 'APPROVED' AND " +
           "((l.fromDate >= :start AND l.fromDate <= :end) OR (l.toDate >= :start AND l.toDate <= :end))")
    List<LeaveRequest> findApprovedLeavesInPeriod(@Param("employeeId") Long employeeId,
                                                  @Param("start") LocalDate start,
                                                  @Param("end") LocalDate end);
}
