package com.system.repository;

import com.system.entity.WhatsAppMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    Page<WhatsAppMessage> findByEmployeeId(Long employeeId, Pageable pageable);
    Boolean existsByWaMessageId(String waMessageId);
}
