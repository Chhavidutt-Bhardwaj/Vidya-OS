-- =============================================================================
-- V2__user_auth_schema.sql
-- User, Role, Permission and Auth tables
--
-- Depends on: V1__school_onboarding_schema.sql  (schools, school_chains)
--
-- Table creation order:
--   1.  permissions
--   2.  roles
--   3.  role_permissions          (join)
--   4.  system_users
--   5.  user_roles                (join)
--   6.  refresh_tokens
--   7.  seed: system roles
--   8.  seed: system permissions
--   9.  seed: role ↔ permission bindings
--  10.  seed: SUPER_ADMIN user
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. permissions
--    Atomic permission codes — format MODULE:ACTION
--    e.g. SCHOOL:READ, ATTENDANCE:MARK, FEE:COLLECT
-- -----------------------------------------------------------------------------
CREATE TABLE permissions (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at  TIMESTAMP       NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP,

    code        VARCHAR(100)    NOT NULL,
    description VARCHAR(255),
    module      VARCHAR(50)     NOT NULL,

    CONSTRAINT pk_permissions       PRIMARY KEY (id),
    CONSTRAINT uq_permission_code   UNIQUE (code)
);

CREATE INDEX idx_perm_code   ON permissions (code);
CREATE INDEX idx_perm_module ON permissions (module);

COMMENT ON TABLE  permissions        IS 'Atomic permission codes assigned to roles';
COMMENT ON COLUMN permissions.code   IS 'Format: MODULE:ACTION — e.g. SCHOOL:READ, FEE:COLLECT';
COMMENT ON COLUMN permissions.module IS 'Logical grouping: school|chain|student|attendance|fee|ai etc.';


-- -----------------------------------------------------------------------------
-- 2. roles
--    Named roles — system roles are shared; school-specific roles have school_id set.
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,

    name            VARCHAR(50)     NOT NULL,
    description     VARCHAR(255),
    school_id       UUID,
    chain_id        UUID,
    is_system_role  BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_roles         PRIMARY KEY (id),
    CONSTRAINT uq_role_name     UNIQUE (name)
);

CREATE INDEX idx_role_name      ON roles (name);
CREATE INDEX idx_role_school_id ON roles (school_id);
CREATE INDEX idx_role_chain_id  ON roles (chain_id);

COMMENT ON TABLE  roles                 IS 'Named roles — SUPER_ADMIN, SCHOOL_ADMIN, TEACHER, PARENT, etc.';
COMMENT ON COLUMN roles.school_id       IS 'NULL for global roles; SET for school-scoped custom roles';
COMMENT ON COLUMN roles.is_system_role  IS 'TRUE = platform-defined, not deletable by clients';


-- -----------------------------------------------------------------------------
-- 3. role_permissions  (many-to-many join)
-- -----------------------------------------------------------------------------
CREATE TABLE role_permissions (
    role_id         UUID    NOT NULL,
    permission_id   UUID    NOT NULL,

    CONSTRAINT pk_role_permissions  PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role           FOREIGN KEY (role_id)
                                        REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission     FOREIGN KEY (permission_id)
                                        REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_rp_role_id       ON role_permissions (role_id);
CREATE INDEX idx_rp_permission_id ON role_permissions (permission_id);


-- -----------------------------------------------------------------------------
-- 4. system_users
--    Every human who can log in — SUPER_ADMIN down to PARENT / STUDENT.
-- -----------------------------------------------------------------------------
CREATE TABLE system_users (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP,
    created_by              UUID,
    updated_by              UUID,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP,

    -- Identity
    email                   VARCHAR(255)    NOT NULL,
    password_hash           VARCHAR(255)    NOT NULL,
    full_name               VARCHAR(150)    NOT NULL,
    phone                   VARCHAR(20),

    -- Tenant scope
    user_type               VARCHAR(30)     NOT NULL,
    school_id               UUID,
    chain_id                UUID,

    -- Status
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    must_change_password    BOOLEAN         NOT NULL DEFAULT FALSE,
    last_login_at           TIMESTAMP,
    password_changed_at     TIMESTAMP,

    CONSTRAINT pk_system_users          PRIMARY KEY (id),
    CONSTRAINT uq_user_email            UNIQUE (email),
    CONSTRAINT chk_user_type            CHECK (user_type IN (
        'SUPER_ADMIN','CHAIN_ADMIN','SCHOOL_ADMIN',
        'PRINCIPAL','TEACHER','PARENT','STUDENT'
    ))
);

CREATE INDEX idx_user_email     ON system_users (email);
CREATE INDEX idx_user_school_id ON system_users (school_id);
CREATE INDEX idx_user_chain_id  ON system_users (chain_id);
CREATE INDEX idx_user_type      ON system_users (user_type);
CREATE INDEX idx_user_active    ON system_users (active);
CREATE INDEX idx_user_deleted   ON system_users (is_deleted);

COMMENT ON TABLE  system_users                       IS 'All platform users — every login identity';
COMMENT ON COLUMN system_users.must_change_password  IS 'TRUE for auto-provisioned accounts — forced reset on first login';
COMMENT ON COLUMN system_users.school_id             IS 'NULL for SUPER_ADMIN and CHAIN_ADMIN';
COMMENT ON COLUMN system_users.password_hash         IS 'BCrypt hash — never store plain text';


-- -----------------------------------------------------------------------------
-- 5. user_roles  (many-to-many join)
-- -----------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id     UUID    NOT NULL,
    role_id     UUID    NOT NULL,

    CONSTRAINT pk_user_roles    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user       FOREIGN KEY (user_id)
                                    REFERENCES system_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role       FOREIGN KEY (role_id)
                                    REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_ur_user_id ON user_roles (user_id);
CREATE INDEX idx_ur_role_id ON user_roles (role_id);


-- -----------------------------------------------------------------------------
-- 6. refresh_tokens
--    Hashed refresh tokens for JWT rotation.
--    Raw token is never stored — only SHA-256 hex hash.
-- -----------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at  TIMESTAMP       NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP,

    user_id     UUID            NOT NULL,
    token_hash  VARCHAR(255)    NOT NULL,
    expires_at  TIMESTAMP       NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,
    device_info VARCHAR(255),

    CONSTRAINT pk_refresh_tokens    PRIMARY KEY (id),
    CONSTRAINT uq_rt_token_hash     UNIQUE (token_hash)
);

CREATE INDEX idx_rt_token_hash  ON refresh_tokens (token_hash);
CREATE INDEX idx_rt_user_id     ON refresh_tokens (user_id);
CREATE INDEX idx_rt_expires_at  ON refresh_tokens (expires_at);
CREATE INDEX idx_rt_revoked     ON refresh_tokens (revoked);

COMMENT ON TABLE  refresh_tokens            IS 'JWT refresh tokens — stored as SHA-256 hash only';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hex digest of the raw token sent to the client';
COMMENT ON COLUMN refresh_tokens.revoked    IS 'TRUE after rotation or explicit logout';


-- =============================================================================
-- SEED DATA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 7. System roles
-- -----------------------------------------------------------------------------
INSERT INTO roles (id, created_at, name, description, is_system_role) VALUES
    (gen_random_uuid(), NOW(), 'SUPER_ADMIN',   'Full platform access — no tenant scope',       TRUE),
    (gen_random_uuid(), NOW(), 'CHAIN_ADMIN',   'Manages all branches within their chain',      TRUE),
    (gen_random_uuid(), NOW(), 'SCHOOL_ADMIN',  'Full access within one school',                TRUE),
    (gen_random_uuid(), NOW(), 'PRINCIPAL',     'Academic head — readonly finance, full academic',TRUE),
    (gen_random_uuid(), NOW(), 'TEACHER',       'Class teacher — own sections only',            TRUE),
    (gen_random_uuid(), NOW(), 'PARENT',        'Read-only access to own child data',           TRUE),
    (gen_random_uuid(), NOW(), 'STUDENT',       'Read-only access to own academic data',        TRUE);


-- -----------------------------------------------------------------------------
-- 8. System permissions
-- -----------------------------------------------------------------------------
INSERT INTO permissions (id, created_at, code, description, module) VALUES
    -- School
    (gen_random_uuid(), NOW(), 'SCHOOL:READ',            'View school details',                  'school'),
    (gen_random_uuid(), NOW(), 'SCHOOL:UPDATE',          'Update school settings and info',      'school'),
    (gen_random_uuid(), NOW(), 'SCHOOL:ONBOARD',         'Onboard a new school',                 'school'),
    (gen_random_uuid(), NOW(), 'SCHOOL:DEACTIVATE',      'Deactivate a school',                  'school'),

    -- Chain
    (gen_random_uuid(), NOW(), 'CHAIN:READ',             'View chain details',                   'chain'),
    (gen_random_uuid(), NOW(), 'CHAIN:MANAGE',           'Create and update chains',             'chain'),
    (gen_random_uuid(), NOW(), 'CHAIN:MANAGE_BRANCHES',  'Add/remove branches in a chain',       'chain'),

    -- Student
    (gen_random_uuid(), NOW(), 'STUDENT:CREATE',         'Enrol a new student',                  'student'),
    (gen_random_uuid(), NOW(), 'STUDENT:READ',           'View student profiles',                'student'),
    (gen_random_uuid(), NOW(), 'STUDENT:UPDATE',         'Update student information',           'student'),
    (gen_random_uuid(), NOW(), 'STUDENT:DELETE',         'Archive a student record',             'student'),

    -- Attendance
    (gen_random_uuid(), NOW(), 'ATTENDANCE:MARK',        'Mark daily attendance',                'attendance'),
    (gen_random_uuid(), NOW(), 'ATTENDANCE:EDIT',        'Correct past attendance entries',      'attendance'),
    (gen_random_uuid(), NOW(), 'ATTENDANCE:REPORT',      'View attendance reports',              'attendance'),

    -- Fee
    (gen_random_uuid(), NOW(), 'FEE:COLLECT',            'Record fee payments',                  'fee'),
    (gen_random_uuid(), NOW(), 'FEE:WAIVE',              'Approve fee waivers',                  'fee'),
    (gen_random_uuid(), NOW(), 'FEE:DEFAULTER_REPORT',   'View fee defaulter reports',           'fee'),
    (gen_random_uuid(), NOW(), 'FEE:STRUCTURE_MANAGE',   'Create and edit fee structures',       'fee'),

    -- Teacher / HR
    (gen_random_uuid(), NOW(), 'TEACHER:ONBOARD',        'Onboard a new teacher',                'hr'),
    (gen_random_uuid(), NOW(), 'TEACHER:READ',           'View staff profiles',                  'hr'),
    (gen_random_uuid(), NOW(), 'TEACHER:UPDATE',         'Update teacher information',           'hr'),
    (gen_random_uuid(), NOW(), 'SALARY:PROCESS',         'Process staff salary disbursement',    'hr'),
    (gen_random_uuid(), NOW(), 'SALARY:VIEW',            'View salary slips and history',        'hr'),

    -- Academic
    (gen_random_uuid(), NOW(), 'ACADEMIC:TIMETABLE',     'Create and edit timetables',           'academic'),
    (gen_random_uuid(), NOW(), 'ACADEMIC:EXAM_MANAGE',   'Schedule exams and enter marks',       'academic'),
    (gen_random_uuid(), NOW(), 'ACADEMIC:REPORT_CARD',   'Generate and publish report cards',    'academic'),

    -- Reports
    (gen_random_uuid(), NOW(), 'REPORT:GENERATE',        'Generate all standard reports',        'report'),
    (gen_random_uuid(), NOW(), 'REPORT:EXPORT',          'Export reports to PDF/Excel',          'report'),

    -- AI
    (gen_random_uuid(), NOW(), 'AI:INSIGHTS',            'View AI-generated school insights',    'ai'),
    (gen_random_uuid(), NOW(), 'AI:REMARK_GENERATE',     'Use AI to generate student remarks',   'ai'),

    -- User management
    (gen_random_uuid(), NOW(), 'USER:MANAGE',            'Create and manage user accounts',      'user'),
    (gen_random_uuid(), NOW(), 'USER:ROLES_ASSIGN',      'Assign roles to users',               'user');


-- -----------------------------------------------------------------------------
-- 9. Role ↔ permission bindings
--    Uses subqueries to keep the seed data readable and order-independent.
-- -----------------------------------------------------------------------------

-- SUPER_ADMIN gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN';

-- CHAIN_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CHAIN_ADMIN'
  AND p.code IN (
    'SCHOOL:READ', 'SCHOOL:UPDATE', 'SCHOOL:ONBOARD',
    'CHAIN:READ', 'CHAIN:MANAGE', 'CHAIN:MANAGE_BRANCHES',
    'STUDENT:READ', 'TEACHER:READ',
    'ATTENDANCE:REPORT', 'FEE:DEFAULTER_REPORT',
    'REPORT:GENERATE', 'REPORT:EXPORT',
    'AI:INSIGHTS', 'USER:MANAGE', 'USER:ROLES_ASSIGN'
  );

-- SCHOOL_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SCHOOL_ADMIN'
  AND p.code IN (
    'SCHOOL:READ', 'SCHOOL:UPDATE',
    'STUDENT:CREATE', 'STUDENT:READ', 'STUDENT:UPDATE', 'STUDENT:DELETE',
    'ATTENDANCE:MARK', 'ATTENDANCE:EDIT', 'ATTENDANCE:REPORT',
    'FEE:COLLECT', 'FEE:WAIVE', 'FEE:DEFAULTER_REPORT', 'FEE:STRUCTURE_MANAGE',
    'TEACHER:ONBOARD', 'TEACHER:READ', 'TEACHER:UPDATE',
    'SALARY:PROCESS', 'SALARY:VIEW',
    'ACADEMIC:TIMETABLE', 'ACADEMIC:EXAM_MANAGE', 'ACADEMIC:REPORT_CARD',
    'REPORT:GENERATE', 'REPORT:EXPORT',
    'AI:INSIGHTS', 'AI:REMARK_GENERATE',
    'USER:MANAGE', 'USER:ROLES_ASSIGN'
  );

-- PRINCIPAL
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'PRINCIPAL'
  AND p.code IN (
    'SCHOOL:READ',
    'STUDENT:READ', 'STUDENT:UPDATE',
    'ATTENDANCE:MARK', 'ATTENDANCE:EDIT', 'ATTENDANCE:REPORT',
    'FEE:DEFAULTER_REPORT',
    'TEACHER:READ',
    'ACADEMIC:TIMETABLE', 'ACADEMIC:EXAM_MANAGE', 'ACADEMIC:REPORT_CARD',
    'REPORT:GENERATE', 'REPORT:EXPORT',
    'AI:INSIGHTS', 'AI:REMARK_GENERATE'
  );

-- TEACHER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TEACHER'
  AND p.code IN (
    'STUDENT:READ',
    'ATTENDANCE:MARK', 'ATTENDANCE:REPORT',
    'ACADEMIC:EXAM_MANAGE', 'ACADEMIC:REPORT_CARD',
    'AI:REMARK_GENERATE'
  );

-- PARENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'PARENT'
  AND p.code IN (
    'STUDENT:READ',
    'ATTENDANCE:REPORT',
    'FEE:DEFAULTER_REPORT',
    'REPORT:GENERATE'
  );

-- STUDENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'STUDENT'
  AND p.code IN (
    'ATTENDANCE:REPORT',
    'REPORT:GENERATE'
  );


-- -----------------------------------------------------------------------------
-- 10. Bootstrap SUPER_ADMIN user
--     Email:    superadmin@vidya.ai
--     Password: SuperAdmin@123  (BCrypt — MUST be rotated before production!)
--     Hash generated with: BCrypt.hashpw("SuperAdmin@123", BCrypt.gensalt(12))
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    v_user_id UUID := gen_random_uuid();
    v_role_id UUID;
BEGIN
    SELECT id INTO v_role_id FROM roles WHERE name = 'SUPER_ADMIN';

    INSERT INTO system_users (
        id, created_at, email, password_hash, full_name,
        user_type, active, must_change_password
    ) VALUES (
        v_user_id,
        NOW(),
        'superadmin@vidya.ai',
        '$2a$12$tGgC/p.7JNLb5i.7YJ4FGeuFcC7ZVk7Ln2SZ7S4BjMv0c/X2G9Y1.',
        'Super Admin',
        'SUPER_ADMIN',
        TRUE,
        TRUE  -- must rotate this password immediately
    );

    INSERT INTO user_roles (user_id, role_id) VALUES (v_user_id, v_role_id);

    RAISE NOTICE 'SUPER_ADMIN user created: superadmin@vidya.ai — ROTATE PASSWORD BEFORE PRODUCTION';
END $$;


-- =============================================================================
-- END OF MIGRATION
-- =============================================================================
