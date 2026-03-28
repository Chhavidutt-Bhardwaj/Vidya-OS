-- =============================================================================
-- V6__student_module.sql
-- Fix PlanType constraint, add Student + Enrollment schema
--
-- Depends on: V5 (school_sections must exist)
-- =============================================================================

-- ── Fix PlanType constraint to match enum ─────────────────────────────────────
ALTER TABLE schools DROP CONSTRAINT IF EXISTS chk_school_plan;
ALTER TABLE schools ADD CONSTRAINT chk_school_plan CHECK (plan IN (
    'FREE', 'STARTER', 'BASIC', 'GROWTH', 'PRO', 'ENTERPRISE', 'CHAIN', 'COACHING'
));

-- ── Fix SchoolType constraint ─────────────────────────────────────────────────
ALTER TABLE schools DROP CONSTRAINT IF EXISTS chk_school_type;
ALTER TABLE schools ADD CONSTRAINT chk_school_type CHECK (type IN (
    'PRIVATE', 'GOVERNMENT', 'GOVERNMENT_AIDED',
    'COACHING_INSTITUTE', 'CHAIN_BRANCH', 'CHAIN_HQ',
    'PRE_PRIMARY', 'PRIMARY', 'MIDDLE', 'SECONDARY',
    'SENIOR_SECONDARY', 'K12', 'JUNIOR_COLLEGE', 'SPECIAL_NEEDS'
));

-- =============================================================================
-- students
-- =============================================================================
CREATE TABLE students (
    id                       UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at               TIMESTAMP       NOT NULL,
    updated_at               TIMESTAMP,
    created_by               UUID,
    updated_by               UUID,
    is_deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMP,

    school_id                UUID            NOT NULL,
    admission_no             VARCHAR(30)     NOT NULL,
    first_name               VARCHAR(80)     NOT NULL,
    middle_name              VARCHAR(80),
    last_name                VARCHAR(80)     NOT NULL,
    gender                   VARCHAR(10)     NOT NULL CHECK (gender IN ('MALE','FEMALE','OTHER')),
    date_of_birth            DATE            NOT NULL,
    blood_group              VARCHAR(15),
    aadhar_no                VARCHAR(12),
    photo_url                VARCHAR(512),
    religion                 VARCHAR(50),
    caste_category           VARCHAR(30),
    nationality              VARCHAR(50)     NOT NULL DEFAULT 'Indian',
    personal_email           VARCHAR(255),
    mobile_no                VARCHAR(15),
    current_section_id       UUID,
    current_roll_no          VARCHAR(20),
    current_academic_year_id UUID,
    admission_date           DATE            NOT NULL,
    admission_class          VARCHAR(50),
    student_status           VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                             CHECK (student_status IN (
                                 'ACTIVE', 'INACTIVE', 'TRANSFERRED',
                                 'LEFT', 'GRADUATED', 'DETAINED'
                             )),
    leaving_date             DATE,
    leaving_reason           VARCHAR(500),

    CONSTRAINT pk_students          PRIMARY KEY (id),
    CONSTRAINT fk_student_school    FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT uq_student_admission UNIQUE (school_id, admission_no)
);

CREATE INDEX idx_student_school_id     ON students(school_id);
CREATE INDEX idx_student_admission_no  ON students(school_id, admission_no);
CREATE INDEX idx_student_status        ON students(student_status);
CREATE INDEX idx_student_section       ON students(current_section_id);

COMMENT ON TABLE  students              IS 'Student master records — one row per student per school';
COMMENT ON COLUMN students.admission_no IS 'School-unique admission number, e.g. ADM-2025-00001';

-- =============================================================================
-- student_guardians
-- =============================================================================
CREATE TABLE student_guardians (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,

    student_id      UUID            NOT NULL,
    relation        VARCHAR(20)     NOT NULL
                    CHECK (relation IN (
                        'FATHER', 'MOTHER', 'GUARDIAN', 'GRANDPARENT',
                        'SIBLING', 'UNCLE', 'AUNT', 'OTHER'
                    )),
    full_name       VARCHAR(150)    NOT NULL,
    mobile          VARCHAR(15)     NOT NULL,
    email           VARCHAR(255),
    occupation      VARCHAR(100),
    aadhar_no       VARCHAR(12),
    annual_income   BIGINT,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    can_pickup      BOOLEAN         NOT NULL DEFAULT TRUE,
    user_id         UUID,

    CONSTRAINT pk_student_guardians PRIMARY KEY (id),
    CONSTRAINT fk_guardian_student  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

CREATE INDEX idx_guardian_student_id ON student_guardians(student_id);

-- Only one primary guardian per student
CREATE UNIQUE INDEX uq_student_primary_guardian
    ON student_guardians(student_id)
    WHERE is_primary = TRUE AND is_deleted = FALSE;

-- =============================================================================
-- student_enrollments
-- =============================================================================
CREATE TABLE student_enrollments (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP,
    created_by        UUID,
    updated_by        UUID,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP,

    student_id        UUID            NOT NULL,
    academic_year_id  UUID            NOT NULL,
    section_id        UUID            NOT NULL,
    roll_no           VARCHAR(20),
    class_name        VARCHAR(50)     NOT NULL,
    section_name      VARCHAR(10)     NOT NULL,
    enrolled_on       DATE            NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN (
                          'ACTIVE', 'PROMOTED', 'DETAINED', 'TRANSFERRED', 'LEFT'
                      )),
    promoted_on       DATE,
    promotion_remarks VARCHAR(500),

    CONSTRAINT pk_student_enrollments  PRIMARY KEY (id),
    CONSTRAINT fk_enroll_student       FOREIGN KEY (student_id)       REFERENCES students(id),
    CONSTRAINT fk_enroll_year          FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_enroll_section       FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT uq_enrollment           UNIQUE (student_id, academic_year_id)
);

CREATE INDEX idx_enroll_student_id  ON student_enrollments(student_id);
CREATE INDEX idx_enroll_section_id  ON student_enrollments(section_id);
CREATE INDEX idx_enroll_year_id     ON student_enrollments(academic_year_id);
CREATE INDEX idx_enroll_status      ON student_enrollments(status);

-- =============================================================================
-- student_documents
-- =============================================================================
CREATE TABLE student_documents (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,

    student_id          UUID            NOT NULL,
    document_type       VARCHAR(50)     NOT NULL,
    document_name       VARCHAR(255)    NOT NULL,
    file_url            VARCHAR(512)    NOT NULL,
    mime_type           VARCHAR(100),
    file_size_bytes     BIGINT,
    original_file_name  VARCHAR(255),
    verified            BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_student_docs      PRIMARY KEY (id),
    CONSTRAINT fk_doc_student       FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

CREATE INDEX idx_student_doc_student_id ON student_documents(student_id);

-- =============================================================================
-- END V6
-- =============================================================================
