package com.servicedeskpro.service.impl;

import com.servicedeskpro.dto.response.AttachmentDto;
import com.servicedeskpro.entity.Attachment;
import com.servicedeskpro.entity.AuditLog;
import com.servicedeskpro.entity.Ticket;
import com.servicedeskpro.entity.User;
import com.servicedeskpro.entity.enums.AuditAction;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.exception.BadRequestException;
import com.servicedeskpro.exception.ForbiddenException;
import com.servicedeskpro.exception.ResourceNotFoundException;
import com.servicedeskpro.repository.AttachmentRepository;
import com.servicedeskpro.repository.AuditLogRepository;
import com.servicedeskpro.repository.TicketRepository;
import com.servicedeskpro.repository.UserRepository;
import com.servicedeskpro.security.UserPrincipal;
import com.servicedeskpro.service.AttachmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.storage.upload-dir:./uploads/attachments}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "pdf", "txt", "log", "doc", "docx", "xls", "xlsx", "zip", "json"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @PostConstruct
    public void init() {
        try {
            Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootPath);
            log.info("Secure attachment storage initialized at: {}", rootPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local attachment storage directory", e);
        }
    }

    @Override
    @Transactional
    public AttachmentDto uploadAttachment(Long ticketId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 10MB");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User currentUser = getCurrentAuthenticatedUser();
        validateTicketAccess(ticket, currentUser);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename containing path traversal characters: " + originalFilename);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(String.format("File extension '.%s' is not permitted. Allowed: %s",
                    extension, ALLOWED_EXTENSIONS));
        }

        // Generate sanitized unique storage filename
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        Path targetPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file " + originalFilename, ex);
        }

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .uploadedBy(currentUser)
                .originalFileName(originalFilename)
                .storedFileName(storedFileName)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileSizeBytes(file.getSize())
                .storagePath(targetPath.toString())
                .build();

        Attachment savedAttachment = attachmentRepository.save(attachment);

        // Audit Trail
        AuditLog auditLog = AuditLog.builder()
                .ticket(ticket)
                .performedBy(currentUser)
                .action(AuditAction.ATTACHMENT_UPLOADED)
                .newValue(originalFilename)
                .notes("Attached file: " + originalFilename + " (" + (file.getSize() / 1024) + " KB)")
                .build();
        auditLogRepository.save(auditLog);

        return mapToDto(savedAttachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> getAttachmentsForTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User currentUser = getCurrentAuthenticatedUser();
        validateTicketAccess(ticket, currentUser);

        return attachmentRepository.findByTicketId(ticketId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        User currentUser = getCurrentAuthenticatedUser();
        validateTicketAccess(attachment.getTicket(), currentUser);

        try {
            Path filePath = Paths.get(attachment.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Attachment file not found on disk: " + attachment.getOriginalFileName());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Error reading file path: " + attachment.getStoragePath(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDto getAttachmentMetadata(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));
        return mapToDto(attachment);
    }

    private String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        return lastIndex == -1 ? "" : filename.substring(lastIndex + 1);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("Unauthenticated user session");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
    }

    private void validateTicketAccess(Ticket ticket, User user) {
        boolean isAgent = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_SUPPORT_AGENT);
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isCreator = ticket.getCreatedBy().getId().equals(user.getId());

        if (!isAgent && !isAdmin && !isCreator) {
            throw new ForbiddenException("You are not authorized to access attachments for this ticket");
        }
    }

    private AttachmentDto mapToDto(Attachment attachment) {
        return AttachmentDto.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket().getId())
                .originalFileName(attachment.getOriginalFileName())
                .storedFileName(attachment.getStoredFileName())
                .contentType(attachment.getContentType())
                .fileSizeBytes(attachment.getFileSizeBytes())
                .uploadedByFullName(attachment.getUploadedBy().getFirstName() + " " + attachment.getUploadedBy().getLastName())
                .uploadedByUsername(attachment.getUploadedBy().getUsername())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}