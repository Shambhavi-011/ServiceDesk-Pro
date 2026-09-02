package com.servicedeskpro.service;

import com.servicedeskpro.dto.response.AuditLogDto;
import com.servicedeskpro.dto.response.PageResponse;

import java.util.List;

public interface AuditLogService {
    List<AuditLogDto> getAuditLogsForTicket(Long ticketId);
    PageResponse<AuditLogDto> getAllAuditLogs(int page, int size);
}