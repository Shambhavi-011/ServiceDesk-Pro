package com.servicedeskpro.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedeskpro.dto.request.CreateCommentRequestDto;
import com.servicedeskpro.entity.Category;
import com.servicedeskpro.entity.Role;
import com.servicedeskpro.entity.Ticket;
import com.servicedeskpro.entity.User;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import com.servicedeskpro.repository.*;
import com.servicedeskpro.security.JwtTokenProvider;
import com.servicedeskpro.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentAndAttachmentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User employee;
    private User otherEmployee;
    private User agent;
    private Category softwareCategory;
    private Ticket ticket;
    private String employeeToken;
    private String otherEmployeeToken;
    private String agentToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        auditLogRepository.deleteAll();
        commentRepository.deleteAll();
        attachmentRepository.deleteAll();
        ticketRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        Role empRole = roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_EMPLOYEE).build()));
        Role agentRole = roleRepository.findByName(RoleName.ROLE_SUPPORT_AGENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_SUPPORT_AGENT).build()));

        employee = userRepository.save(User.builder()
                .username("david.emp")
                .email("david.emp@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("David")
                .lastName("Miller")
                .department("Finance")
                .active(true)
                .roles(Set.of(empRole))
                .build());

        otherEmployee = userRepository.save(User.builder()
                .username("emma.emp")
                .email("emma.emp@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Emma")
                .lastName("Watson")
                .department("Legal")
                .active(true)
                .roles(Set.of(empRole))
                .build());

        agent = userRepository.save(User.builder()
                .username("sam.agent")
                .email("sam.agent@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Sam")
                .lastName("Wilson")
                .department("IT Ops")
                .active(true)
                .roles(Set.of(agentRole))
                .build());

        softwareCategory = categoryRepository.save(Category.builder()
                .name("Software")
                .description("Software license and access")
                .defaultSlaHours(12)
                .active(true)
                .build());

        ticket = ticketRepository.save(Ticket.builder()
                .ticketNumber("TCK-20260901-5501")
                .title("IntelliJ IDEA License Renewal")
                .description("License expired yesterday")
                .status(TicketStatus.ASSIGNED)
                .priority(TicketPriority.MEDIUM)
                .category(softwareCategory)
                .createdBy(employee)
                .assignedTo(agent)
                .build());

        employeeToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(employee));
        otherEmployeeToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(otherEmployee));
        agentToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(agent));
    }

    @Test
    @DisplayName("Comments: Public comment vs Agent Internal Note visibility")
    void testCommentPublicVsInternalNotes() throws Exception {
        // 1. Employee posts public comment
        CreateCommentRequestDto publicComment = CreateCommentRequestDto.builder()
                .content("Attaching the screenshot of license error")
                .internalNote(false)
                .build();

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publicComment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.internalNote").value(false));

        // 2. Employee tries to post internal note -> 403 Forbidden
        CreateCommentRequestDto illegalInternal = CreateCommentRequestDto.builder()
                .content("Trying to sneak an internal note")
                .internalNote(true)
                .build();

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalInternal)))
                .andExpect(status().isForbidden());

        // 3. Agent posts internal note
        CreateCommentRequestDto agentInternal = CreateCommentRequestDto.builder()
                .content("Checked license server; need to allocate pool B key")
                .internalNote(true)
                .build();

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentInternal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.internalNote").value(true));

        // 4. Employee fetches comments -> Expect only 1 comment (Internal note hidden)
        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].content").value("Attaching the screenshot of license error"));

        // 5. Agent fetches comments -> Expect 2 comments (both public + internal note visible)
        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/comments")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("Attachments: Upload, Metadata, Unauthorized check & Secure Download")
    void testSecureAttachmentWorkflow() throws Exception {
        // 1. Employee uploads error log
        MockMultipartFile logFile = new MockMultipartFile(
                "file",
                "license_error.log",
                "text/plain",
                "ERROR 2026-09-01: License validation key expired".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/tickets/" + ticket.getId() + "/attachments")
                        .file(logFile)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalFileName").value("license_error.log"))
                .andExpect(jsonPath("$.data.storedFileName").isNotEmpty())
                .andReturn();

        JsonNode attachJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("data");
        Long attachmentId = attachJson.get("id").asLong();

        // 2. Unauthorized employee tries to download -> Expect 403 Forbidden
        mockMvc.perform(get("/api/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + otherEmployeeToken))
                .andExpect(status().isForbidden());

        // 3. Authorized Agent downloads attachment -> Expect 200 OK with correct stream & header
        MvcResult downloadResult = mockMvc.perform(get("/api/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"license_error.log\""))
                .andReturn();

        byte[] downloadedBytes = downloadResult.getResponse().getContentAsByteArray();
        assertThat(new String(downloadedBytes)).contains("ERROR 2026-09-01: License validation key expired");

        // 4. Verify audit log entry for attachment upload
        var auditLogs = auditLogRepository.findByTicketIdOrderByTimestampDesc(ticket.getId());
        assertThat(auditLogs.stream().anyMatch(a -> a.getNotes().contains("Attached file: license_error.log"))).isTrue();
    }
}