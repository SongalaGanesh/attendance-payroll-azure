-- Drop tables if they exist (for easy setup)
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS conversation_state;
DROP TABLE IF EXISTS whatsapp_messages;
DROP TABLE IF EXISTS payroll;
DROP TABLE IF EXISTS leave_history;
DROP TABLE IF EXISTS leave_requests;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS designations;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;

-- 1. USERS
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_username (username)
);

-- 2. ROLES
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 3. USER_ROLES
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 4. DEPARTMENTS
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE
);

-- 5. DESIGNATIONS
CREATE TABLE designations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

-- 6. EMPLOYEES
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    mobile_number VARCHAR(20) NOT NULL,
    whatsapp_number VARCHAR(20) NOT NULL UNIQUE,
    department_id BIGINT,
    designation_id BIGINT,
    user_id BIGINT UNIQUE,
    joining_date DATE NOT NULL,
    monthly_salary DECIMAL(15, 2) NOT NULL,
    office_latitude DECIMAL(10, 8) NOT NULL,
    office_longitude DECIMAL(11, 8) NOT NULL,
    allowed_radius_meters INT DEFAULT 100,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (designation_id) REFERENCES designations(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_emp_code (employee_code),
    INDEX idx_emp_whatsapp (whatsapp_number)
);

-- 7. ATTENDANCE
CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in_time TIME NULL,
    check_out_time TIME NULL,
    check_in_lat DECIMAL(10, 8) NULL,
    check_in_lng DECIMAL(11, 8) NULL,
    check_out_lat DECIMAL(10, 8) NULL,
    check_out_lng DECIMAL(11, 8) NULL,
    distance_meters DECIMAL(10, 2) NULL,
    total_working_hours DECIMAL(5, 2) NULL,
    attendance_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_emp_date (employee_id, attendance_date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_attendance_date (attendance_date)
);

-- 8. LEAVE_REQUESTS
CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    total_days INT NOT NULL,
    reason TEXT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_leave_status (status)
);

-- 9. LEAVE_HISTORY
CREATE TABLE leave_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_request_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    actioned_by BIGINT NULL,
    remarks TEXT NULL,
    actioned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (leave_request_id) REFERENCES leave_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (actioned_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 10. PAYROLL
CREATE TABLE payroll (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    working_days INT NOT NULL,
    present_days INT NOT NULL,
    approved_leave_days INT NOT NULL,
    absent_days INT NOT NULL,
    gross_salary DECIMAL(15, 2) NOT NULL,
    net_salary DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'GENERATED',
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_emp_month_year (employee_id, month, year),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_payroll_period (year, month)
);

-- 11. WHATSAPP_MESSAGES
CREATE TABLE whatsapp_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NULL,
    direction VARCHAR(10) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    message_body TEXT NULL,
    wa_message_id VARCHAR(255) NOT NULL UNIQUE,
    message_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    INDEX idx_wa_msg_timestamp (message_timestamp)
);

-- 12. CONVERSATION_STATE
CREATE TABLE conversation_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL UNIQUE,
    current_state VARCHAR(50) NOT NULL,
    context_data TEXT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 13. AUDIT_LOGS
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_values TEXT NULL,
    new_values TEXT NULL,
    ip_address VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- SEED ROLES
INSERT INTO roles (name, description) VALUES 
('ROLE_SUPER_ADMIN', 'Super Administrator with full credentials'),
('ROLE_HR_ADMIN', 'HR Administrator managing employee and payroll workflows'),
('ROLE_EMPLOYEE', 'Standard employee dashboard access');

-- SEED DEPARTMENTS
INSERT INTO departments (name, code, is_active) VALUES
('Human Resources', 'HR', TRUE),
('Engineering', 'ENG', TRUE),
('Operations', 'OPS', TRUE);

-- SEED DESIGNATIONS
INSERT INTO designations (title, department_id) VALUES
('HR Manager', 1),
('Software Engineer', 2),
('Operations Executive', 3);

-- SEED INITIAL SUPER USER
-- Password hash corresponds to 'admin123'
INSERT INTO users (username, email, password_hash, is_active) VALUES
('admin', 'admin@company.com', '$2a$10$Isau2KwT33bf3Gq4ZRBDaO0Afz5qC1OaYZENtPBZ9Jds4xgGxsrZe', TRUE);

-- MAP SUPER USER TO SUPER_ADMIN ROLE
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);
