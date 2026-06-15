package com.system.repository;

import com.system.entity.Payroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    Optional<Payroll> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);
    
    Page<Payroll> findByMonthAndYear(Integer month, Integer year, Pageable pageable);
    
    Page<Payroll> findByEmployeeId(Long employeeId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.netSalary), 0) FROM Payroll p WHERE p.month = :month AND p.year = :year")
    BigDecimal sumNetSalaryByMonthAndYear(@Param("month") Integer month, @Param("year") Integer year);
}
