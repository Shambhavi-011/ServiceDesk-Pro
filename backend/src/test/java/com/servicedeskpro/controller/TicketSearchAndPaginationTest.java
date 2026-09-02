package com.servicedeskpro.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketSearchAndPaginationTest {

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

    private User employee1;
    private User employee2;
    private User agent;
    private Category networkCategory;
    private Category hardwareCategory;
    private String employee1Token;
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

        employee1 = userRepository.save(User.builder()
                .username("alex.emp")
                .email("alex.emp@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Alex")
                .lastName("Turner")
                .department("Engineering")
                .active(true)
                .roles(Set.of(empRole))
                .build());

        employee2 = userRepository.save(User.builder()
                .username("bella.emp")
                .email("bella.emp@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Bella")
                .lastName("Goth")
                .department("Marketing")
                .active(true)
                .roles(Set.of(empRole))
                .build());

        agent = userRepository.save(User.builder()
                .username("victor.agent")
                .email("victor.agent@company.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .firstName("Victor")
                .lastName("Stone")
                .department("IT Operations")
                .active(true)
                .roles(Set.of(agentRole))
                .build());

        networkCategory = categoryRepository.save(Category.builder()
                .name("Network")
                .description("VPN, Wi-Fi, switches")
                .defaultSlaHours(8)
                .active(true)
                .build());

        hardwareCategory = categoryRepository.save(Category.builder()
                .name("Hardware")
                .description("Physical devices")
                .defaultSlaHours(24)
                .active(true)
                .build());

        employee1Token = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(employee1));
        agentToken = jwtTokenProvider.generateTokenFromUserPrincipal(UserPrincipal.create(agent));

        // Seed 5 Tickets
        // Ticket 1 (Employee 1, Network, Critical, Open, Unassigned)
        ticketRepository.save(Ticket.builder()
                .ticketNumber("TCK-20260901-0001")
                .title("VPN Gateway connection timeout")
                .description("Cannot reach corporate internal subnet via VPN")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.CRITICAL)
                .category(networkCategory)
                .createdBy(employee1)
                .build());

        // Ticket 2 (Employee 1, Hardware, Low, In Progress, Assigned to Victor)
        ticketRepository.save(Ticket.builder()
                .ticketNumber("TCK-20260901-0002")
                .title("Ergonomic mouse request")
                .description("Need wireless vertical mouse")
                .status(TicketStatus.IN_PROGRESS)
                .priority(TicketPriority.LOW)
                .category(hardwareCategory)
                .createdBy(employee1)
                .assignedTo(agent)
                .build());

        // Ticket 3 (Employee 2, Network, High, Open, Unassigned)
        ticketRepository.save(Ticket.builder()
                .ticketNumber("TCK-20260901-0003")
                .title("Office Wi-Fi password prompt looping")
                .description("Office Wi-Fi certificate failed on Mac")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .category(networkCategory)
                .createdBy(employee2)
                .build());

        // Ticket 4 (Employee 2, Hardware, Medium, Resolved, Assigned to Victor)
        ticketRepository.save(Ticket.builder()
                .ticketNumber("TCK-20260901-0004")
                .title("Second Monitor HDMI cable replacement")
                .description("Flickering screen cable")
                .status(TicketStatus.RESOLVED)
                .priority(TicketPriority.MEDIUM)
                .category(hardwareCategory)
                .createdBy(employee2)
                .assignedTo(agent)
                .build());

        // Ticket 5 (Employee 2, Hardware, Critical, Open, Unassigned)
        ticketRepository.save(Ticket.builder()
                .ticketNumber("TCK-20260901-0005")
                .title("Executive board room projector failed")
                .description("Projector bulb exploded before VP meeting")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.CRITICAL)
                .category(hardwareCategory)
                .createdBy(employee2)
                .build());
    }

    @Test
    @DisplayName("Employee role scoping: Employee only receives own tickets")
    void testEmployeeRoleScoping() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(json.get("content").size()).isEqualTo(2);
        assertThat(json.get("content").get(0).get("createdByUsername").asText()).isEqualTo("alex.emp");
    }

    @Test
    @DisplayName("Agent search & multi-criteria filtering: Status + Priority + Category")
    void testAgentFilterStatusAndPriority() throws Exception {
        mockMvc.perform(get("/api/tickets?status=OPEN&priority=CRITICAL")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].priority").value("CRITICAL"))
                .andExpect(jsonPath("$.data.content[1].priority").value("CRITICAL"));
    }

    @Test
    @DisplayName("Agent substring keyword search: search='VPN'")
    void testAgentKeywordSearch() throws Exception {
        mockMvc.perform(get("/api/tickets?search=VPN")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].ticketNumber").value("TCK-20260901-0001"));
    }

    @Test
    @DisplayName("Agent Unassigned Queue filter: unassigned=true")
    void testAgentUnassignedQueue() throws Exception {
        mockMvc.perform(get("/api/tickets?unassigned=true")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    @DisplayName("Pagination & Sorting: page=0, size=2, sortBy=createdAt, sortDir=asc")
    void testPaginationAndSorting() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tickets?page=0&size=2&sortBy=createdAt&sortDir=asc")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.last").value(false))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(json.get("content").size()).isEqualTo(2);
    }
}