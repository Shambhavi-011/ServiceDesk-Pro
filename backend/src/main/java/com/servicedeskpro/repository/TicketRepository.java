package com.servicedeskpro.repository;

import com.servicedeskpro.entity.Ticket;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @EntityGraph(attributePaths = {"category", "createdBy", "assignedTo"})
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    @Override
    @EntityGraph(attributePaths = {"category", "createdBy", "assignedTo"})
    Optional<Ticket> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "createdBy", "assignedTo"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    long countByStatus(TicketStatus status);

    long countByAssignedToIdAndStatus(Long assignedToId, TicketStatus status);

    long countByCreatedByIdAndStatus(Long createdById, TicketStatus status);

    long countByPriority(TicketPriority priority);

    @Query("SELECT t FROM Ticket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') AND t.slaDueAt < :now")
    List<Ticket> findOverdueTickets(Instant now);
}