-- V1 数据库结构

-- 用户账户表 (核心认证)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE,
    username VARCHAR(50) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_phone_or_username CHECK (
        phone IS NOT NULL OR username IS NOT NULL
    )
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

-- 插入基础角色
INSERT INTO roles (name, description) VALUES
    ('admin', '考务人员'),
    ('student', '考生'),
    ('teacher', '教师/监考')
ON CONFLICT (name) DO NOTHING;
