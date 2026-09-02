package com.servicedeskpro.repository;

import com.servicedeskpro.entity.*;
import com.servicedeskpro.entity.enums.AuditAction;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Role employeeRole;
    private Role agentRole;
    private User employee;
    private User agent;
    private Category hardwareCategory;

    @BeforeEach
    void setUp() {
        employeeRole = entityManager.persist(Role.builder().name(RoleName.ROLE_EMPLOYEE).build());
        agentRole = entityManager.persist(Role.builder().name(RoleName.ROLE_SUPPORT_AGENT).build());

        employee = entityManager.persist(User.builder()
                .username("john.doe")
                .email("john.doe@company.com")
                .passwordHash("hashed_pw_123")
                .firstName("John")
                .lastName("Doe")
                .department("Engineering")
                .roles(Set.of(employeeRole))
                .build());

        agent = entityManager.persist(User.builder()
                .username("sarah.agent")
                .email("sarah.agent@company.com")
                .passwordHash("hashed_pw_456")
                .firstName("Sarah")
                .lastName("Connor")
                .department("IT Support")
                .roles(Set.of(agentRole))
                .build());

        hardwareCategory = entityManager.persist(Category.builder()
                .name("Hardware")
                .description("Hardware support")
                .defaultSlaHours(24)
                .build());

        entityManager.flush();
    }

    @Test
    @DisplayName("UserRepository: findByUsername returns user with eagerly loaded roles")
    void testFindUserByUsername() {
        Optional<User> found = userRepository.findByUsername("john.doe");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john.doe@company.com");
        assertThat(found.get().getRoles()).hasSize(1);
        assertThat(found.get().getRoles().iterator().next().getName()).isEqualTo(RoleName.ROLE_EMPLOYEE);
    }

    @Test
    @DisplayName("TicketRepository: persist and retrieve ticket with lifecycle state transitions")
    void testTicketPersistenceAndStateTransitions() {
        Ticket ticket = Ticket.builder()
                .ticketNumber("TCK-20260901-001")
                .title("Broken Laptop Screen")
                .description("Screen flickers on boot")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .category(hardwareCategory)
                .createdBy(employee)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        entityManager.flush();
        entityManager.clear();

        Optional<Ticket> retrieved = ticketRepository.findByTicketNumber("TCK-20260901-001");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTitle()).isEqualTo("Broken Laptop Screen");
        assertThat(retrieved.get().getStatus()).isEqualTo(TicketStatus.OPEN);

        // Verify Enum state transition business rule
        assertThat(retrieved.get().getStatus().canTransitionTo(TicketStatus.ASSIGNED)).isTrue();
        assertThat(retrieved.get().getStatus().canTransitionTo(TicketStatus.CLOSED)).isTrue();
        assertThat(retrieved.get().getStatus().canTransitionTo(TicketStatus.RESOLVED)).isFalse(); // Direct resolve not allowed from OPEN
    }

    @Test
    @DisplayName("CommentRepository & AuditLogRepository: persist comments and immutable audit trail")
    void testCommentsAndAuditLogging() {
        Ticket ticket = entityManager.persist(Ticket.builder()
                .ticketNumber("TCK-20260901-002")
                .title("Need secondary monitor")
                .description("Working on dual screen setup")
                .status(TicketStatus.ASSIGNED)
                .priority(TicketPriority.MEDIUM)
                .category(hardwareCategory)
                .createdBy(employee)
                .assignedTo(agent)
                .build());

        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(agent)
                .content("Monitor request approved. Dispatched from IT store.")
                .internalNote(false)
                .build();
        commentRepository.save(comment);

        AuditLog auditLog = AuditLog.builder()
                .ticket(ticket)
                .performedBy(agent)
                .action(AuditAction.AGENT_ASSIGNED)
                .oldValue("UNASSIGNED")
                .newValue(agent.getUsername())
                .notes("Auto-assigned to on-duty agent")
                .build();
        auditLogRepository.save(auditLog);

        entityManager.flush();
        entityManager.clear();

        var comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getContent()).contains("Monitor request approved");

        var auditLogs = auditLogRepository.findByTicketIdOrderByTimestampDesc(ticket.getId());
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getAction()).isEqualTo(AuditAction.AGENT_ASSIGNED);
        assertThat(auditLogs.get(0).getNewValue()).isEqualTo("sarah.agent");
    }
}