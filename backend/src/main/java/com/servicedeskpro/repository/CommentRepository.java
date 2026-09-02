package com.servicedeskpro.repository;

import com.servicedeskpro.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(Long ticketId);
}