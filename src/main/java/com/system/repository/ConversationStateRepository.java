package com.system.repository;

import com.system.entity.ConversationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConversationStateRepository extends JpaRepository<ConversationState, Long> {
    Optional<ConversationState> findByEmployeeId(Long employeeId);
    void deleteByEmployeeId(Long employeeId);
}
