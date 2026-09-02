package com.servicedeskpro.repository;

import com.servicedeskpro.entity.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @EntityGraph(attributePaths = {"uploadedBy"})
    List<Attachment> findByTicketId(Long ticketId);

    Optional<Attachment> findByStoredFileName(String storedFileName);
}