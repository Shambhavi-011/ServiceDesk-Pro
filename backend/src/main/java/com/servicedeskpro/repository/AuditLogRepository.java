package com.servicedeskpro.repository;

import com.servicedeskpro.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = {"performedBy"})
    List<AuditLog> findByTicketIdOrderByTimestampDesc(Long ticketId);

    @EntityGraph(attributePaths = {"ticket", "performedBy"})
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}