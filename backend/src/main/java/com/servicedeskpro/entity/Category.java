package com.servicedeskpro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 50, unique = true, nullable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "default_sla_hours", nullable = false)
    @Builder.Default
    private Integer defaultSlaHours = 24;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}