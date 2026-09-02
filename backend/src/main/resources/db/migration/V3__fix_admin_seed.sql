-- =========================================================================
-- Flyway Migration V3: Fix Admin Seed Data (MySQL quoting fix)
-- =========================================================================
-- This migration corrects data that may have been incorrectly inserted
-- by V2 due to PostgreSQL-style quoting in MySQL.
-- =========================================================================

-- Fix roles (re-insert with correct values if missing)
INSERT INTO roles (id, name) VALUES
(1, 'ROLE_EMPLOYEE'),
(2, 'ROLE_SUPPORT_AGENT'),
(3, 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Fix categories (re-insert if missing due to prior quoting bug)
INSERT INTO categories (name, description, default_sla_hours, is_active) VALUES
('Hardware', 'Laptops, monitors, peripherals, docking stations, physical repairs', 24, TRUE),
('Software & Tools', 'IDE licenses, VPN client, enterprise software installation', 12, TRUE),
('Network & Connectivity', 'Wi-Fi access, office LAN, VPN connectivity issues', 8, TRUE),
('Account Access & IAM', 'Password resets, Single Sign-On (SSO), Active Directory permissions', 4, TRUE),
('Security Incident', 'Phishing reports, suspicious activity, device encryption', 2, TRUE),
('General IT Request', 'Workstation ergonomics, IT asset handover, miscellaneous', 48, TRUE)
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Fix/create admin user with verified BCrypt hash for Admin@123
INSERT INTO users (id, username, email, password_hash, first_name, last_name, department, is_active)
VALUES (
    1,
    'admin',
    'admin@servicedeskpro.local',
    '$2a$10$.qMY.KvszaqwbbFJw9WkROOgGOcJQdWUp3v5mQLzjdGLKMe0rmgEe',
    'System',
    'Administrator',
    'IT Infrastructure',
    TRUE
)
ON DUPLICATE KEY UPDATE
    username      = 'admin',
    password_hash = '$2a$10$.qMY.KvszaqwbbFJw9WkROOgGOcJQdWUp3v5mQLzjdGLKMe0rmgEe',
    is_active     = TRUE;

-- Fix user_roles mapping
INSERT INTO user_roles (user_id, role_id)
VALUES (1, 3)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
