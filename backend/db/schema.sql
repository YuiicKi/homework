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
    ('role.read', '查看角色列表')
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
    'role.read'
)
WHERE r.name = 'admin'
ON CONFLICT DO NOTHING;
