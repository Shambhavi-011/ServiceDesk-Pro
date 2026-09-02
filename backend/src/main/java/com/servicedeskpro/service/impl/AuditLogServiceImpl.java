package com.servicedeskpro.service.impl;

import com.servicedeskpro.dto.response.AuditLogDto;
import com.servicedeskpro.dto.response.PageResponse;
import com.servicedeskpro.entity.AuditLog;
import com.servicedeskpro.repository.AuditLogRepository;
import com.servicedeskpro.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditLogsForTicket(Long ticketId) {
        return auditLogRepository.findByTicketIdOrderByTimestampDesc(ticketId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> getAllAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logPage = auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        return PageResponse.of(logPage.map(this::mapToDto));
    }

    private AuditLogDto mapToDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .ticketId(log.getTicket().getId())
                .ticketNumber(log.getTicket().getTicketNumber())
                .performedById(log.getPerformedBy().getId())
                .performedByFullName(log.getPerformedBy().getFirstName() + " " + log.getPerformedBy().getLastName())
                .performedByUsername(log.getPerformedBy().getUsername())
                .action(log.getAction())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .notes(log.getNotes())
                .timestamp(log.getTimestamp())
                .build();
    }
}