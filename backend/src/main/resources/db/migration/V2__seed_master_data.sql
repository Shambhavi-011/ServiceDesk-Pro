-- =========================================================================
-- Flyway Migration V2: Seed Master Roles, Default Categories & Seed Admin
-- =========================================================================

-- 1. Seed Roles
INSERT INTO roles (id, name) VALUES
(1, 'ROLE_EMPLOYEE'),
(2, 'ROLE_SUPPORT_AGENT'),
(3, 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. Seed Default IT Service Categories with Enterprise SLA targets
INSERT INTO categories (name, description, default_sla_hours, is_active) VALUES
('Hardware', 'Laptops, monitors, peripherals, docking stations, physical repairs', 24, TRUE),
('Software & Tools', 'IDE licenses, VPN client, enterprise software installation', 12, TRUE),
('Network & Connectivity', 'Wi-Fi access, office LAN, VPN connectivity issues', 8, TRUE),
('Account Access & IAM', 'Password resets, Single Sign-On (SSO), Active Directory permissions', 4, TRUE),
('Security Incident', 'Phishing reports, suspicious activity, device encryption', 2, TRUE),
('General IT Request', 'Workstation ergonomics, IT asset handover, miscellaneous', 48, TRUE)
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 3. Seed Default Administrator Account
-- Password: Admin@123  |  BCrypt cost-10 hash verified by Spring Security BCryptPasswordEncoder
INSERT INTO users (id, username, email, password_hash, first_name, last_name, department, is_active) VALUES
(1, 'admin', 'admin@servicedeskpro.local',
 '$2a$10$.qMY.KvszaqwbbFJw9WkROOgGOcJQdWUp3v5mQLzjdGLKMe0rmgEe',
 'System', 'Administrator', 'IT Infrastructure', TRUE)
ON DUPLICATE KEY UPDATE email = VALUES(email);

-- 4. Assign ROLE_ADMIN to default admin user
INSERT INTO user_roles (user_id, role_id) VALUES (1, 3)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);