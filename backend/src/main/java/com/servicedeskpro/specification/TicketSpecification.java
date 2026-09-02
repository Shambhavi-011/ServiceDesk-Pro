package com.servicedeskpro.specification;

import com.servicedeskpro.entity.Ticket;
import com.servicedeskpro.entity.User;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class TicketSpecification {

    private TicketSpecification() {}

    public static Specification<Ticket> buildFilter(
            User currentUser,
            TicketStatus status,
            TicketPriority priority,
            Long categoryId,
            Long assignedToId,
            Long createdById,
            String searchTerm,
            Boolean myTicketsOnly,
            Boolean unassignedOnly) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean isAgent = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_SUPPORT_AGENT);
            boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

            // 1. Role-Based Scoping: Employees can ONLY view tickets they created
            if (!isAgent && !isAdmin) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), currentUser.getId()));
            } else {
                // If Agent/Admin explicitly requested "myTicketsOnly"
                if (Boolean.TRUE.equals(myTicketsOnly)) {
                    if (isAgent) {
                        predicates.add(cb.equal(root.get("assignedTo").get("id"), currentUser.getId()));
                    } else {
                        predicates.add(cb.equal(root.get("createdBy").get("id"), currentUser.getId()));
                    }
                }
            }

            // 2. Unassigned Queue Filter (For Agents/Admins)
            if (Boolean.TRUE.equals(unassignedOnly)) {
                predicates.add(cb.isNull(root.get("assignedTo")));
            }

            // 3. Status Filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 4. Priority Filter
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            // 5. Category Filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // 6. Assigned Agent Filter
            if (assignedToId != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToId));
            }

            // 7. Created By Filter (Admin view)
            if (createdById != null && (isAdmin || isAgent)) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), createdById));
            }

            // 8. Keyword Substring Search (across title, description, and ticketNumber)
            if (StringUtils.hasText(searchTerm)) {
                String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate ticketNumLike = cb.like(cb.lower(root.get("ticketNumber")), pattern);

                predicates.add(cb.or(titleLike, descLike, ticketNumLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}