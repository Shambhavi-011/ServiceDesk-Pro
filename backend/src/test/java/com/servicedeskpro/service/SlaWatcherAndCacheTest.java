package com.servicedeskpro.service;

import com.servicedeskpro.dto.request.CategoryRequestDto;
import com.servicedeskpro.dto.response.CategoryDto;
import com.servicedeskpro.entity.*;
import com.servicedeskpro.entity.enums.AuditAction;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import com.servicedeskpro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SlaWatcherAndCacheTest {

    @Autowired
    private SlaBreachWatcherService slaBreachWatcherService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CacheManager cacheManager;

    private User employee;
    private User agent;
    private Category category;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        auditLogRepository.deleteAll();
        ticketRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        if (cacheManager.getCache("categories") != null) {
            cacheManager.getCache("categories").clear();
        }

        Role empRole = roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_EMPLOYEE).build()));
        Role agentRole = roleRepository.findByName(RoleName.ROLE_SUPPORT_AGENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_SUPPORT_AGENT).build()));

        employee = userRepository.save(User.builder()
                .username("john.test")
                .email("john.test@company.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .department("Sales")
                .active(true)
                .roles(Set.of(empRole))
                .build());

        agent = userRepository.save(User.builder()
                .username("agent.test")
                .email("agent.test@company.com")
                .passwordHash("hashed")
                .firstName("Agent")
                .lastName("Smith")
                .department("IT Operations")
                .active(true)
                .roles(Set.of(agentRole))
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Workstation Support")
                .defaultSlaHours(8)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("SLA Watcher: automatically flags expired tickets, escalates priority to CRITICAL, and logs audit alert")
    void testSlaBreachDetectionAndEscalation() {
        // Seed a ticket that expired 2 hours ago with MEDIUM priority
        Ticket expiredTicket = ticketRepository.save(Ticket.builder()
                .ticketNumber("TCK-20260901-SLA01")
                .title("Expired Laptop Screen Glitch")
                .description("Monitor flickering intermittently")
                .status(TicketStatus.IN_PROGRESS)
                .priority(TicketPriority.MEDIUM)
                .category(category)
                .createdBy(employee)
                .assignedTo(agent)
                .slaDueAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build());

        // Run SLA Watcher background scan
        int breachedCount = slaBreachWatcherService.checkAndFlagSlaBreaches();

        assertThat(breachedCount).isEqualTo(1);

        // Verify ticket priority was escalated to CRITICAL
        Ticket updatedTicket = ticketRepository.findById(expiredTicket.getId()).orElseThrow();
        assertThat(updatedTicket.getPriority()).isEqualTo(TicketPriority.CRITICAL);

        // Verify audit log entry
        List<AuditLog> auditLogs = auditLogRepository.findByTicketIdOrderByTimestampDesc(expiredTicket.getId());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getAction()).isEqualTo(AuditAction.PRIORITY_CHANGED);
        assertThat(auditLogs.get(0).getNewValue()).isEqualTo("CRITICAL");

        // Verify notification to assigned agent
        List<Notification> agentNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(agent.getId());
        assertThat(agentNotifs).hasSize(1);
        assertThat(agentNotifs.get(0).getTitle()).contains("SLA BREACH");
    }

    @Test
    @DisplayName("Spring Cache: caches Category queries and evicts cache on mutations")
    void testCategoryCachingAndEviction() {
        // Initial fetch: populates cache
        List<CategoryDto> firstFetch = categoryService.getAllActiveCategories();
        assertThat(firstFetch).hasSize(1);

        // Verify cache contains entry
        assertThat(cacheManager.getCache("categories")).isNotNull();
        assertThat(cacheManager.getCache("categories").get("allActive")).isNotNull();

        // Create new category -> triggers @CacheEvict
        categoryService.createCategory(CategoryRequestDto.builder()
                .name("Network Infrastructure")
                .description("VPN and router issues")
                .defaultSlaHours(12)
                .build());

        // Second fetch: should reflect fresh database state (2 categories)
        List<CategoryDto> secondFetch = categoryService.getAllActiveCategories();
        assertThat(secondFetch).hasSize(2);
    }
}