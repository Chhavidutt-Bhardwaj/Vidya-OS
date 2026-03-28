-- =============================================================================
-- V4__platform_admin_role.sql
-- Adds the PLATFORM_ADMIN role for the Vidya backend/ops team.
--
-- Depends on: V2__auth_and_user_schema.sql (roles, permissions tables must exist)
--
-- SUPER_ADMIN   → full platform access, including user management and schema ops
--                 (for founders / CTO — break-glass access)
--
-- PLATFORM_ADMIN → operational admin for the backend team:
--                   - Can onboard and manage schools and chains
--                   - Can view all data across tenants
--                   - Cannot manage user accounts or assign roles
--                   - Cannot access AI features (reserved for school staff)
--                 This is the role to give to your support / ops engineers.
--
-- The actual SystemUser rows are NOT created here — they are created by
-- PlatformAdminSeeder (ApplicationRunner) from vidyaos.platform-admins config.
-- Keeping user creation out of migrations ensures credentials come from
-- environment variables, never from version-controlled SQL.
-- =============================================================================

-- ── Add PLATFORM_ADMIN role ───────────────────────────────────────────────────

INSERT INTO roles (id, created_at, name, description, is_system_role)
VALUES (
           gen_random_uuid(),
           NOW(),
           'PLATFORM_ADMIN',
           'Vidya ops/support team — full school management, no user/role management',
           TRUE
       );

-- ── Assign permissions ────────────────────────────────────────────────────────
-- Gets everything EXCEPT:
--   USER:MANAGE        — cannot create/edit user accounts
--   USER:ROLES_ASSIGN  — cannot assign roles
--   AI:INSIGHTS        — AI features are for school staff, not platform team
--   AI:REMARK_GENERATE — same

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r,
       permissions p
WHERE  r.name = 'PLATFORM_ADMIN'
  AND  p.code IN (
    -- School
                  'SCHOOL:READ',
                  'SCHOOL:UPDATE',
                  'SCHOOL:ONBOARD',
                  'SCHOOL:DEACTIVATE',
    -- Chain
                  'CHAIN:READ',
                  'CHAIN:MANAGE',
                  'CHAIN:MANAGE_BRANCHES',
    -- Student (read + manage for support)
                  'STUDENT:CREATE',
                  'STUDENT:READ',
                  'STUDENT:UPDATE',
                  'STUDENT:DELETE',
    -- Attendance
                  'ATTENDANCE:MARK',
                  'ATTENDANCE:EDIT',
                  'ATTENDANCE:REPORT',
    -- Fee
                  'FEE:COLLECT',
                  'FEE:WAIVE',
                  'FEE:DEFAULTER_REPORT',
                  'FEE:STRUCTURE_MANAGE',
    -- HR
                  'TEACHER:ONBOARD',
                  'TEACHER:READ',
                  'TEACHER:UPDATE',
                  'SALARY:PROCESS',
                  'SALARY:VIEW',
    -- Academic
                  'ACADEMIC:TIMETABLE',
                  'ACADEMIC:EXAM_MANAGE',
                  'ACADEMIC:REPORT_CARD',
    -- Reports
                  'REPORT:GENERATE',
                  'REPORT:EXPORT'
    -- Excluded: USER:MANAGE, USER:ROLES_ASSIGN, AI:INSIGHTS, AI:REMARK_GENERATE
    );

COMMENT ON TABLE roles IS
    'System and custom roles. SUPER_ADMIN = break-glass. PLATFORM_ADMIN = ops team daily driver.';

-- =============================================================================
-- END OF MIGRATION
-- =============================================================================