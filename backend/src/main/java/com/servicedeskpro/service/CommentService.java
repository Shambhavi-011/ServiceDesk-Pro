package com.servicedeskpro.service;

import com.servicedeskpro.dto.request.CreateCommentRequestDto;
import com.servicedeskpro.dto.response.CommentDto;

import java.util.List;

public interface CommentService {
    CommentDto addComment(Long ticketId, CreateCommentRequestDto request);
    List<CommentDto> getCommentsForTicket(Long ticketId);
}