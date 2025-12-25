-- V1 数据库结构

-- 用户账户表 (核心认证)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    token_version INTEGER NOT NULL DEFAULT 0
);

-- 角色表 (定义系统中有哪些角色)
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- 用户-角色关联表 (多对多)
CREATE TABLE IF NOT EXISTS user_roles_link (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 考生信息扩展表
CREATE TABLE IF NOT EXISTS student_profiles (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    id_card_number VARCHAR(18),
    photo_url VARCHAR(255)
);

-- 教师/监考信息扩展表
CREATE TABLE IF NOT EXISTS teacher_profiles (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    staff_id VARCHAR(50),
    school_or_department VARCHAR(100)
);

-- 考务人员信息扩展表
CREATE TABLE IF NOT EXISTS admin_profiles (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    staff_id VARCHAR(50),
    department VARCHAR(100)
);

-- 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    description TEXT
);

-- 角色与权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 插入基础角色
INSERT INTO roles (name, description) VALUES
    ('admin', '考务人员'),
    ('student', '考生'),
    ('teacher', '教师/监考')
ON CONFLICT (name) DO NOTHING;

-- 插入基础权限
INSERT INTO permissions (code, description) VALUES
    ('user.create', '创建用户'),
    ('user.update', '更新用户'),
    ('user.delete', '删除用户'),
    ('user.read', '查看单个用户'),
    ('user.read.all', '查看用户列表'),
    ('role.create', '创建角色'),
    ('role.update', '更新角色'),
    ('role.delete', '删除角色'),
    ('role.assign', '分配或移除角色'),
    ('role.read', '查看角色列表'),
    ('center.create', '创建考点'),
    ('center.update', '更新考点'),
    ('center.read', '查看考点'),
    ('room.create', '创建考场'),
    ('room.update', '更新考场'),
    ('room.read', '查看考场'),
    ('room.status.update', '更新考场状态'),
    ('subject.create', '创建科目'),
    ('subject.update', '更新科目'),
    ('subject.read', '查看科目'),
    ('subject.status.update', '更新科目状态'),
    ('subject.delete', '删除科目'),
    ('subject.import', '导入科目'),
    ('subject.export', '导出科目'),
    ('registration.create', '创建或更新报名时间'),
    ('registration.status.update', '更新报名时间状态'),
    ('registration.delete', '删除报名时间'),
    ('registration.read', '查看报名时间'),
    ('registration.export', '导出报名时间'),
    ('notification.create', '创建通知'),
    ('notification.update', '更新通知'),
    ('notification.publish', '发布通知'),
    ('notification.withdraw', '撤回通知'),
    ('notification.read', '查看通知'),
    ('notification.template', '通知模板管理'),
    ('notification.log', '查看通知日志'),
    ('registration.material.template', '报名材料模板管理'),
    ('registration.audit', '报名审核'),
    ('session.create', '创建场次'),
    ('session.update', '更新场次'),
    ('session.read', '查看场次'),
    ('schedule.create', '创建排考'),
    ('schedule.update', '更新排考'),
    ('schedule.status.update', '更新排考状态'),
    ('schedule.delete', '删除排考'),
    ('schedule.read', '查看排考'),
    ('result.manage', '成绩管理')
ON CONFLICT (code) DO NOTHING;

-- 默认授予管理员全部权限
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
    'center.create',
    'center.update',
    'center.read',
    'room.create',
    'room.update',
    'room.read',
    'room.status.update',
    'subject.create',
    'subject.update',
    'subject.read',
    'subject.status.update',
    'subject.delete',
    'subject.import',
    'subject.export',
    'registration.create',
    'registration.status.update',
    'registration.delete',
    'registration.read',
    'registration.export',
    'notification.create',
    'notification.update',
    'notification.publish',
    'notification.withdraw',
    'notification.read',
    'notification.template',
    'notification.log',
    'registration.material.template',
    'registration.audit',
    'session.create',
    'session.update',
    'session.read',
    'schedule.create',
    'schedule.update',
    'schedule.status.update',
    'schedule.delete',
    'schedule.read',
    'result.manage'
)
WHERE r.name = 'admin'
ON CONFLICT DO NOTHING;

-- 授予学生基本权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'notification.read'
)
WHERE r.name = 'student'
ON CONFLICT DO NOTHING;

-- 考点
CREATE TABLE IF NOT EXISTS exam_centers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    description TEXT
);

-- 考场
CREATE TABLE IF NOT EXISTS exam_rooms (
    id SERIAL PRIMARY KEY,
    center_id INTEGER NOT NULL REFERENCES exam_centers(id) ON DELETE CASCADE,
    room_number VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    capacity INTEGER,
    location TEXT,
    manager_name VARCHAR(100),
    manager_phone VARCHAR(50),
    UNIQUE (center_id, room_number)
);

-- 考场状态日志
CREATE TABLE IF NOT EXISTS exam_room_status_logs (
    id SERIAL PRIMARY KEY,
    room_id INTEGER NOT NULL REFERENCES exam_rooms(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 科目
CREATE TABLE IF NOT EXISTS exam_subjects (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ENABLED',
    duration_minutes INTEGER NOT NULL DEFAULT 0,
    question_count INTEGER NOT NULL DEFAULT 0,
    description TEXT
);

-- 科目日志
CREATE TABLE IF NOT EXISTS exam_subject_logs (
    id SERIAL PRIMARY KEY,
    subject_id INTEGER NOT NULL REFERENCES exam_subjects(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50),
    reason TEXT,
    operator_id INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 场次
CREATE TABLE IF NOT EXISTS exam_sessions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    note TEXT
);

-- 排考关联
CREATE TABLE IF NOT EXISTS exam_schedules (
    id SERIAL PRIMARY KEY,
    room_id INTEGER NOT NULL REFERENCES exam_rooms(id) ON DELETE CASCADE,
    subject_id INTEGER NOT NULL REFERENCES exam_subjects(id) ON DELETE CASCADE,
    session_id INTEGER NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    note TEXT,
    UNIQUE (room_id, session_id)
);

-- 报名时间配置
CREATE TABLE IF NOT EXISTS exam_registration_windows (
    id SERIAL PRIMARY KEY,
    subject_id INTEGER NOT NULL REFERENCES exam_subjects(id) ON DELETE CASCADE,
    session_id INTEGER NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ENABLED',
    note TEXT
);

-- 报名时间状态日志
CREATE TABLE IF NOT EXISTS exam_registration_window_logs (
    id SERIAL PRIMARY KEY,
    registration_window_id INTEGER NOT NULL REFERENCES exam_registration_windows(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50),
    reason TEXT,
    operator_id INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 通知主表
CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    type VARCHAR(100) NOT NULL,
    content TEXT,
    channel VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    scheduled_at TIMESTAMPTZ,
    created_by INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 通知模板
CREATE TABLE IF NOT EXISTS notification_templates (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    variables TEXT
);

-- 通知目标范围
CREATE TABLE IF NOT EXISTS notification_targets (
    id SERIAL PRIMARY KEY,
    notification_id INTEGER NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    target_type VARCHAR(100) NOT NULL,
    target_value VARCHAR(200)
);

-- 通知发送日志
CREATE TABLE IF NOT EXISTS notification_logs (
    id SERIAL PRIMARY KEY,
    notification_id INTEGER NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    channel VARCHAR(100) NOT NULL,
    target VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 报名信息
CREATE TABLE IF NOT EXISTS registration_info (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id INTEGER NOT NULL REFERENCES exam_subjects(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    id_card_number VARCHAR(30) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    phone VARCHAR(50),
    email VARCHAR(100),
    status VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_registration_user_subject ON registration_info(user_id, subject_id);

-- 报名材料
CREATE TABLE IF NOT EXISTS registration_materials (
    id SERIAL PRIMARY KEY,
    registration_info_id INTEGER NOT NULL REFERENCES registration_info(id) ON DELETE CASCADE,
    type VARCHAR(100) NOT NULL,
    file_url TEXT NOT NULL,
    file_format VARCHAR(50),
    file_size BIGINT,
    status VARCHAR(50),
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

-- 报名审核日志
CREATE TABLE IF NOT EXISTS registration_audit_logs (
    id SERIAL PRIMARY KEY,
    registration_info_id INTEGER NOT NULL REFERENCES registration_info(id) ON DELETE CASCADE,
    result VARCHAR(50) NOT NULL,
    reason TEXT,
    operator_id INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 报名材料模板/要求
CREATE TABLE IF NOT EXISTS registration_material_templates (
    id SERIAL PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    allowed_formats TEXT,
    max_size BIGINT,
    required BOOLEAN DEFAULT FALSE,
    description TEXT
);

-- 成绩信息主表
CREATE TABLE IF NOT EXISTS exam_result_records (
    id SERIAL PRIMARY KEY,
    registration_info_id INTEGER NOT NULL REFERENCES registration_info(id) ON DELETE CASCADE,
    exam_type VARCHAR(150) NOT NULL,
    exam_year INTEGER NOT NULL,
    ticket_number VARCHAR(100),
    release_time TIMESTAMPTZ,
    total_score NUMERIC(10,2),
    total_pass_line NUMERIC(10,2),
    qualification_status VARCHAR(50),
    qualification_note TEXT,
    report_url TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    UNIQUE (registration_info_id, exam_type, exam_year)
);

-- 成绩科目明细
CREATE TABLE IF NOT EXISTS exam_result_details (
    id SERIAL PRIMARY KEY,
    result_id INTEGER NOT NULL REFERENCES exam_result_records(id) ON DELETE CASCADE,
    subject_id INTEGER REFERENCES exam_subjects(id),
    subject_name VARCHAR(150),
    score NUMERIC(10,2),
    pass_line NUMERIC(10,2),
    is_pass BOOLEAN,
    national_rank INTEGER,
    remark TEXT
);

-- 成绩查询预告
CREATE TABLE IF NOT EXISTS exam_result_pre_notifications (
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

-- 成绩发布时间设置
CREATE TABLE IF NOT EXISTS exam_result_release_settings (
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

-- 成绩导入任务
CREATE TABLE IF NOT EXISTS exam_result_import_jobs (
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

-- 成绩导入明细
CREATE TABLE IF NOT EXISTS exam_result_import_items (
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

-- 监考任务分配
CREATE TABLE IF NOT EXISTS exam_invigilator_assignments (
    id SERIAL PRIMARY KEY,
    schedule_id INTEGER NOT NULL REFERENCES exam_schedules(id) ON DELETE CASCADE,
    teacher_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by INTEGER,
    assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (schedule_id, teacher_user_id)
);
