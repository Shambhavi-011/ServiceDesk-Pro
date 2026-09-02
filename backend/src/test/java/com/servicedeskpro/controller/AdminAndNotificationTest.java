package com.servicedeskpro.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedeskpro.dto.request.CreateAgentRequestDto;
import com.servicedeskpro.dto.request.UpdateUserStatusDto;
import com.servicedeskpro.entity.*;
import com.servicedeskpro.entity.enums.AuditAction;
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
class AdminAndNotificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TicketRepository ticketRepository;

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
    private User agent;
    private User admin;
    private String employeeToken;
    private String agentToken;
    private String adminToken;

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
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_ADMIN).build()));

        employee = userRepository.save(User.builder()
                .username("tim.emp")
                .email("tim.emp@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Tim")
                .lastName("Cook")
                .department("Operations")
                .active(true)
                .roles(Set.of(empRole))
                .build());

        agent = userRepository.save(User.builder()
                .username("anna.agent")
                .email("anna.agent@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Anna")
                .lastName("Kendrick")
                .department("IT Services")
                .active(true)
                .roles(Set.of(agentRole))
                .build());

        admin = userRepository.save(User.builder()
                .username("master.admin")
                .email("master.admin@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Master")
                .lastName("Admin")
                .department("IT Governance")
                .active(true)
                .roles(Set.of(adminRole))
                .build());

        employeeToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(employee));
        agentToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(agent));
        adminToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(admin));
    }

    @Test
    @DisplayName("In-App Notifications: read unread list, mark single as read, mark all as read")
    void testNotificationFlow() throws Exception {
        // Seed 2 notifications for employee
        Notification n1 = notificationRepository.save(Notification.builder()
                .recipient(employee)
                .title("Ticket Assigned")
                .message("Your ticket has been assigned to Anna")
                .read(false)
                .build());

        Notification n2 = notificationRepository.save(Notification.builder()
                .recipient(employee)
                .title("Ticket Status Changed")
                .message("Your ticket is now IN_PROGRESS")
                .read(false)
                .build());

        // 1. Get Unread Notifications
        mockMvc.perform(get("/api/notifications?unreadOnly=true")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 2. Check Unread Count
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        // 3. Mark first notification as read
        mockMvc.perform(patch("/api/notifications/" + n1.getId() + "/read")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // 4. Mark all as read
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // 5. Verify Unread Count is now 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    @DisplayName("Operational Dashboard: verifies customized metrics per user role")
    void testDashboardRoleMetrics() throws Exception {
        Category cat = categoryRepository.save(Category.builder().name("General IT").defaultSlaHours(24).active(true).build());

        ticketRepository.save(Ticket.builder()
                .ticketNumber("TCK-20260901-D001")
                .title("Email sync issue")
                .description("Outlook not updating inbox")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.CRITICAL)
                .category(cat)
                .createdBy(employee)
                .build());

        // 1. Employee Dashboard View
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userRole").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.metrics.myOpenTickets").value(1));

        // 2. Support Agent Dashboard View
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userRole").value("SUPPORT_AGENT"))
                .andExpect(jsonPath("$.data.metrics.unassignedQueue").value(1))
                .andExpect(jsonPath("$.data.metrics.criticalTickets").value(1));

        // 3. Admin Dashboard View
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.metrics.totalTickets").value(1))
                .andExpect(jsonPath("$.data.metrics.totalUsers").value(3));
    }

    @Test
    @DisplayName("Admin Governance: provision agent, deactivate user, and view audit history")
    void testAdminGovernance() throws Exception {
        // 1. Employee attempting to access /api/admin/agents -> 403 Forbidden
        CreateAgentRequestDto newAgent = CreateAgentRequestDto.builder()
                .username("bruce.agent")
                .email("bruce.agent@company.com")
                .password("AgentSecure@2026")
                .firstName("Bruce")
                .lastName("Banner")
                .department("IT Infrastructure")
                .build();

        mockMvc.perform(post("/api/admin/agents")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAgent)))
                .andExpect(status().isForbidden());

        // 2. Admin provisions new agent -> 201 Created
        mockMvc.perform(post("/api/admin/agents")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAgent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("bruce.agent"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_SUPPORT_AGENT"));

        // 3. Admin deactivates employee account
        mockMvc.perform(patch("/api/admin/users/" + employee.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusDto(false))))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(employee.getId()).orElseThrow();
        assertThat(updatedUser.isActive()).isFalse();

        // 4. Admin views system audit logs
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}