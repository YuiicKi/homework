CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    token_version INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE user_roles_link (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE student_profiles (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    id_card_number VARCHAR(18),
    photo_url VARCHAR(255)
);

CREATE TABLE teacher_profiles (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    staff_id VARCHAR(50),
    school_or_department VARCHAR(100)
);

CREATE TABLE admin_profiles (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    staff_id VARCHAR(50),
    department VARCHAR(100)
);

CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE role_permissions (
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO roles (name, description) VALUES
('admin', '考务人员'),
('student', '考生'),
('teacher', '教师/监考')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (code, description) VALUES
('user.create', '创建用户'),
('user.update', '更新用户'),
('user.delete', '删除用户'),
('user.read', '查看单个用户'),
('user.read.all', '查看用户列表'),
('role.create', '创建角色'),
('role.update', '更新角色'),
('role.delete', '删除角色'),
('role.assign', '分配角色'),
('role.read', '查看角色列表'),
('result.manage', '成绩管理')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user.create',
    'user.update',
    'user.delete',
    'user.read',
    'user.read.all',
    'role.create',
    'role.update',
    'role.delete',
    'role.assign',
    'role.read',
    'result.manage'
)
WHERE r.name = 'admin'
ON CONFLICT DO NOTHING;

-- 添加 invigilator 角色
INSERT INTO roles (name, description) VALUES
('invigilator', '监考员')
ON CONFLICT (name) DO NOTHING;

-- 考试中心表
CREATE TABLE exam_centers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    capacity INTEGER,
    contact_phone VARCHAR(20),
    contact_person VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 考场表
CREATE TABLE exam_rooms (
    id SERIAL PRIMARY KEY,
    center_id INTEGER NOT NULL REFERENCES exam_centers(id) ON DELETE CASCADE,
    room_number VARCHAR(50) NOT NULL,
    building VARCHAR(100),
    floor INTEGER,
    capacity INTEGER NOT NULL DEFAULT 30,
    has_air_conditioning BOOLEAN DEFAULT FALSE,
    has_projector BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 考试科目表
CREATE TABLE exam_subjects (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 120,
    question_count INTEGER,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 考试场次表
CREATE TABLE exam_sessions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 考试安排表
CREATE TABLE exam_schedules (
    id SERIAL PRIMARY KEY,
    room_id INTEGER NOT NULL REFERENCES exam_rooms(id) ON DELETE CASCADE,
    subject_id INTEGER NOT NULL REFERENCES exam_subjects(id) ON DELETE CASCADE,
    session_id INTEGER NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    exam_date DATE NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 报名窗口表
CREATE TABLE exam_registration_windows (
    id SERIAL PRIMARY KEY,
    subject_id INTEGER NOT NULL REFERENCES exam_subjects(id) ON DELETE CASCADE,
    registration_start_time TIMESTAMPTZ NOT NULL,
    registration_end_time TIMESTAMPTZ NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 报名信息表
CREATE TABLE registration_info (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    subject_id INTEGER NOT NULL REFERENCES exam_subjects(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    id_card_number VARCHAR(18) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    email VARCHAR(100),
    address VARCHAR(255),
    phone VARCHAR(20),
    education VARCHAR(50),
    work_unit VARCHAR(100),
    work_experience VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reject_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 报名材料模板表
CREATE TABLE registration_material_templates (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_required BOOLEAN DEFAULT TRUE,
    file_type VARCHAR(50),
    max_size BIGINT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 座位分配表
CREATE TABLE seat_assignments (
    id SERIAL PRIMARY KEY,
    registration_id INTEGER NOT NULL REFERENCES registration_info(id) ON DELETE CASCADE,
    schedule_id INTEGER NOT NULL REFERENCES exam_schedules(id) ON DELETE CASCADE,
    seat_number VARCHAR(20) NOT NULL,
    ticket_number VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 成绩记录表
CREATE TABLE exam_result_records (
    id SERIAL PRIMARY KEY,
    registration_info_id INTEGER REFERENCES registration_info(id) ON DELETE SET NULL,
    full_name VARCHAR(100) NOT NULL,
    id_card_number VARCHAR(18),
    exam_type VARCHAR(150),
    exam_year INTEGER,
    ticket_number VARCHAR(100),
    release_time TIMESTAMPTZ,
    total_score DECIMAL(10,2),
    total_pass_line DECIMAL(10,2),
    qualification_status VARCHAR(50),
    qualification_note TEXT,
    report_url VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 成绩明细表
CREATE TABLE exam_result_details (
    id SERIAL PRIMARY KEY,
    result_record_id INTEGER NOT NULL REFERENCES exam_result_records(id) ON DELETE CASCADE,
    subject_id INTEGER REFERENCES exam_subjects(id) ON DELETE SET NULL,
    subject_name VARCHAR(100) NOT NULL,
    score DECIMAL(10,2),
    pass_line DECIMAL(10,2),
    is_pass BOOLEAN,
    national_rank INTEGER,
    remark TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 证书表
CREATE TABLE exam_certificates (
    id SERIAL PRIMARY KEY,
    result_record_id INTEGER REFERENCES exam_result_records(id) ON DELETE SET NULL,
    full_name VARCHAR(100) NOT NULL,
    id_card_number VARCHAR(18),
    exam_type VARCHAR(150),
    exam_year INTEGER,
    ticket_number VARCHAR(100),
    certificate_number VARCHAR(100) UNIQUE,
    file_url VARCHAR(255),
    qr_content TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 监考员表
CREATE TABLE invigilators (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    work_unit VARCHAR(100),
    qualification VARCHAR(100),
    experience TEXT,
    specialties TEXT[],
    max_assignments_per_day INTEGER DEFAULT 2,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 监考员可用性表
CREATE TABLE invigilator_availability (
    id SERIAL PRIMARY KEY,
    invigilator_id INTEGER NOT NULL REFERENCES invigilators(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    max_assignments INTEGER DEFAULT 2,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 监考员统计表
CREATE TABLE invigilator_statistics (
    id SERIAL PRIMARY KEY,
    invigilator_id INTEGER NOT NULL REFERENCES invigilators(id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    month INTEGER,
    total_assignments INTEGER DEFAULT 0,
    completed_assignments INTEGER DEFAULT 0,
    cancelled_assignments INTEGER DEFAULT 0,
    total_hours DECIMAL(10,2) DEFAULT 0,
    average_rating DECIMAL(3,2),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 通知表
CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50),
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    target_audience VARCHAR(50),
    target_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    publish_time TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT FALSE,
    created_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 用户通知关联表
CREATE TABLE user_notifications (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_id INTEGER NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 监考员通知表
CREATE TABLE invigilator_notifications (
    id SERIAL PRIMARY KEY,
    invigilator_id INTEGER NOT NULL REFERENCES invigilators(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exam_result_pre_notifications (
    id SERIAL PRIMARY KEY,
    exam_type VARCHAR(150) NOT NULL,
    exam_year INTEGER NOT NULL,
    query_time TIMESTAMPTZ NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    last_notification_id INTEGER,
    last_published_at TIMESTAMPTZ,
    created_by INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE TABLE exam_result_release_settings (
    id SERIAL PRIMARY KEY,
    subject_id INTEGER NOT NULL REFERENCES exam_subjects(id) ON DELETE CASCADE,
    exam_year INTEGER NOT NULL,
    release_time TIMESTAMPTZ NOT NULL,
    pre_notice_offset_minutes INTEGER,
    pre_notice_triggered_at TIMESTAMPTZ,
    auto_release_triggered_at TIMESTAMPTZ,
    pre_notification_id INTEGER REFERENCES exam_result_pre_notifications(id) ON DELETE SET NULL,
    created_by INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    UNIQUE (subject_id, exam_year)
);

CREATE TABLE exam_result_import_jobs (
    id SERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(20),
    status VARCHAR(50) NOT NULL,
    total_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    failure_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_by INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE TABLE exam_result_import_items (
    id SERIAL PRIMARY KEY,
    job_id INTEGER NOT NULL REFERENCES exam_result_import_jobs(id) ON DELETE CASCADE,
    row_number INTEGER,
    registration_info_id INTEGER,
    ticket_number VARCHAR(100),
    subject_id INTEGER,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exam_invigilator_assignments (
    id SERIAL PRIMARY KEY,
    schedule_id INTEGER NOT NULL REFERENCES exam_schedules(id) ON DELETE CASCADE,
    teacher_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by INTEGER,
    assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (schedule_id, teacher_user_id)
);

-- 监考分配表（新版）
CREATE TABLE invigilator_assignments (
    id SERIAL PRIMARY KEY,
    invigilator_id INTEGER NOT NULL REFERENCES invigilators(id) ON DELETE CASCADE,
    schedule_id INTEGER NOT NULL REFERENCES exam_schedules(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'PRIMARY',
    assignment_date DATE,
    status VARCHAR(50) DEFAULT 'ASSIGNED',
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);
