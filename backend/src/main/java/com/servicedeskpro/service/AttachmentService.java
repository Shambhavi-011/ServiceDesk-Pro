package com.servicedeskpro.service;

import com.servicedeskpro.dto.response.AttachmentDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {
    AttachmentDto uploadAttachment(Long ticketId, MultipartFile file);
    List<AttachmentDto> getAttachmentsForTicket(Long ticketId);
    Resource downloadAttachment(Long attachmentId);
    AttachmentDto getAttachmentMetadata(Long attachmentId);
}