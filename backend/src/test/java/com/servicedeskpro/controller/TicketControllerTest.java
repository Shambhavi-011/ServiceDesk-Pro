package com.servicedeskpro.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedeskpro.dto.request.*;
import com.servicedeskpro.entity.Category;
import com.servicedeskpro.entity.Role;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTest {

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User employeeUser;
    private User otherEmployeeUser;
    private User agentUser;
    private Category hardwareCategory;
    private String employeeToken;
    private String otherEmployeeToken;
    private String agentToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        auditLogRepository.deleteAll();
        ticketRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        Role empRole = roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_EMPLOYEE).build()));
        Role agentRole = roleRepository.findByName(RoleName.ROLE_SUPPORT_AGENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_SUPPORT_AGENT).build()));

        employeeUser = userRepository.save(User.builder()
                .username("john.emp")
                .email("john.emp@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("John")
                .lastName("Doe")
                .department("Sales")
                .active(true)
                .roles(Set.of(empRole))
                .build());

        otherEmployeeUser = userRepository.save(User.builder()
                .username("jane.emp")
                .email("jane.emp@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Jane")
                .lastName("Smith")
                .department("HR")
                .active(true)
                .roles(Set.of(empRole))
                .build());

        agentUser = userRepository.save(User.builder()
                .username("mark.agent")
                .email("mark.agent@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Mark")
                .lastName("Taylor")
                .department("IT Support")
                .active(true)
                .roles(Set.of(agentRole))
                .build());

        hardwareCategory = categoryRepository.save(Category.builder()
                .name("Hardware")
                .description("Hardware related issues")
                .defaultSlaHours(24)
                .active(true)
                .build());

        employeeToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(employeeUser));
        otherEmployeeToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(otherEmployeeUser));
        agentToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(agentUser));
    }

    @Test
    @DisplayName("Complete Lifecycle: Create -> Assign -> In Progress -> Resolve -> Reopen -> Close")
    void testCompleteTicketLifecycleWorkflow() throws Exception {
        // 1. Employee Creates Ticket
        CreateTicketRequestDto createDto = CreateTicketRequestDto.builder()
                .title("Docking Station USB-C not charging")
                .description("Laptop does not receive charge when connected to docking station.")
                .categoryId(hardwareCategory.getId())
                .priority(TicketPriority.HIGH)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.ticketNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.slaDueAt").isNotEmpty())
                .andReturn();

        JsonNode ticketJson = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        Long ticketId = ticketJson.get("id").asLong();

        // 2. Other employee tries to read ticket -> Expect 403 Forbidden
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + otherEmployeeToken))
                .andExpect(status().isForbidden());

        // 3. Agent self-assigns ticket
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTicketRequestDto(null, "Self assigned"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.assignedTo.username").value("mark.agent"));

        // 4. Invalid state transition: OPEN/ASSIGNED directly to RESOLVED without IN_PROGRESS -> 409 Conflict
        ResolveTicketRequestDto invalidResolve = ResolveTicketRequestDto.builder()
                .resolutionNotes("Fast resolved")
                .build();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/resolve")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidResolve)))
                .andExpect(status().isConflict());

        // 5. Agent moves status to IN_PROGRESS
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequestDto(TicketStatus.IN_PROGRESS, "Diagnosing power adapter"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // 6. Agent resolves ticket
        ResolveTicketRequestDto validResolve = ResolveTicketRequestDto.builder()
                .resolutionNotes("Replaced 100W USB-C power brick with new OEM unit. Verified charging.")
                .build();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/resolve")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResolve)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionNotes").isNotEmpty())
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        // 7. Employee reopens ticket (issue reoccurred)
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/reopen")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReopenTicketRequestDto("Still drops connection periodically"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REOPENED"));

        // 8. Agent resolves ticket again
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/resolve")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveTicketRequestDto("Replaced docking station motherboard firmware"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        // 9. Employee verifies and closes ticket
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/close")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());

        // 10. Verify that Audit Logs were recorded for all transitions
        var auditLogs = auditLogRepository.findByTicketIdOrderByTimestampDesc(ticketId);
        assertThat(auditLogs.size()).isGreaterThanOrEqualTo(6);

        // 11. Verify that Notifications were created for the employee
        var notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeUser.getId());
        assertThat(notifications).isNotEmpty();
    }
}