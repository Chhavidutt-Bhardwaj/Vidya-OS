-- =============================================================================
-- V5__academic_classes_subjects.sql
-- Classes, sections, subject definitions, and teacher assignments.
--
-- Depends on:
--   V1 (schools, academic_years, school_shifts)
--   V2 (system_users)
--
-- Table creation order:
--   1. subjects
--   2. school_classes
--   3. school_sections
--   4. class_subjects
--   5. section_subject_teachers
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. subjects
--    Subject definitions per school. e.g. Mathematics, Physics, Hindi
-- -----------------------------------------------------------------------------
CREATE TABLE subjects (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMP       NOT NULL,
    updated_at                  TIMESTAMP,
    created_by                  UUID,
    updated_by                  UUID,
    is_deleted                  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMP,

    school_id                   UUID            NOT NULL,
    code                        VARCHAR(20)     NOT NULL,
    name                        VARCHAR(100)    NOT NULL,
    short_name                  VARCHAR(30),
    subject_type                VARCHAR(20)     NOT NULL,
    theory_periods_per_week     INTEGER,
    practical_periods_per_week  INTEGER         NOT NULL DEFAULT 0,
    max_theory_marks            INTEGER,
    max_practical_marks         INTEGER,
    is_graded                   BOOLEAN         NOT NULL DEFAULT FALSE,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    board_override              VARCHAR(30),
    color_hex                   VARCHAR(10),

    CONSTRAINT pk_subjects              PRIMARY KEY (id),
    CONSTRAINT uq_subject_school_code   UNIQUE (school_id, code),
    CONSTRAINT fk_subject_school        FOREIGN KEY (school_id)
                                            REFERENCES schools (id)
                                            ON DELETE CASCADE,
    CONSTRAINT chk_subject_type         CHECK (subject_type IN (
        'CORE','ELECTIVE','LANGUAGE','VOCATIONAL','CO_CURRICULAR'
    )),
    CONSTRAINT chk_theory_marks         CHECK (max_theory_marks IS NULL OR max_theory_marks >= 0),
    CONSTRAINT chk_practical_marks      CHECK (max_practical_marks IS NULL OR max_practical_marks >= 0),
    CONSTRAINT chk_theory_periods       CHECK (theory_periods_per_week IS NULL OR theory_periods_per_week >= 0),
    CONSTRAINT chk_practical_periods    CHECK (practical_periods_per_week >= 0)
);

CREATE INDEX idx_subject_school_id  ON subjects (school_id);
CREATE INDEX idx_subject_type       ON subjects (subject_type);
CREATE INDEX idx_subject_active     ON subjects (active);

COMMENT ON TABLE  subjects              IS 'Subject definitions per school — shared across classes';
COMMENT ON COLUMN subjects.code         IS 'Unique short code within school e.g. MATH, PHY, ENG-A';
COMMENT ON COLUMN subjects.is_graded    IS 'TRUE = grade letters (A/B/C), FALSE = numeric marks';


-- -----------------------------------------------------------------------------
-- 2. school_classes
--    One grade per academic year per school. e.g. "Class 10" in "2024-25"
-- -----------------------------------------------------------------------------
CREATE TABLE school_classes (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,

    school_id           UUID            NOT NULL,
    academic_year_id    UUID            NOT NULL,
    name                VARCHAR(50)     NOT NULL,
    display_name        VARCHAR(30),
    grade_order         INTEGER         NOT NULL DEFAULT 0,
    room                VARCHAR(30),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_school_classes        PRIMARY KEY (id),
    CONSTRAINT uq_class_year_name       UNIQUE (academic_year_id, name),
    CONSTRAINT fk_class_school          FOREIGN KEY (school_id)
                                            REFERENCES schools (id)
                                            ON DELETE CASCADE,
    CONSTRAINT fk_class_academic_year   FOREIGN KEY (academic_year_id)
                                            REFERENCES academic_years (id)
                                            ON DELETE CASCADE,
    CONSTRAINT chk_grade_order          CHECK (grade_order >= 0)
);

CREATE INDEX idx_class_school_id    ON school_classes (school_id);
CREATE INDEX idx_class_year_id      ON school_classes (academic_year_id);
CREATE INDEX idx_class_grade_order  ON school_classes (grade_order);
CREATE INDEX idx_class_active       ON school_classes (active);

COMMENT ON TABLE  school_classes              IS 'One class per grade per academic year';
COMMENT ON COLUMN school_classes.grade_order  IS '0=Nursery, 1=KG1, 2=KG2, 3=Class1 ... 14=Class12';
COMMENT ON COLUMN school_classes.display_name IS 'Short form for timetable: X, XII, Nur';


-- -----------------------------------------------------------------------------
-- 3. school_sections
--    Divisions of a class: A, B, C or Morning/Afternoon for shift schools
-- -----------------------------------------------------------------------------
CREATE TABLE school_sections (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,

    class_id            UUID            NOT NULL,
    name                VARCHAR(10)     NOT NULL,
    class_teacher_id    UUID,           -- references system_users.id (no FK — cross-module)
    room                VARCHAR(30),
    capacity            INTEGER         NOT NULL DEFAULT 40,
    student_count       INTEGER         NOT NULL DEFAULT 0,
    shift_id            UUID,           -- optional override of academic year default shift
    active              BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_school_sections       PRIMARY KEY (id),
    CONSTRAINT uq_section_class_name    UNIQUE (class_id, name),
    CONSTRAINT fk_section_class         FOREIGN KEY (class_id)
                                            REFERENCES school_classes (id)
                                            ON DELETE CASCADE,
    CONSTRAINT fk_section_shift         FOREIGN KEY (shift_id)
                                            REFERENCES school_shifts (id)
                                            ON DELETE SET NULL,
    CONSTRAINT chk_capacity             CHECK (capacity > 0),
    CONSTRAINT chk_student_count        CHECK (student_count >= 0),
    CONSTRAINT chk_student_le_capacity  CHECK (student_count <= capacity)
);

CREATE INDEX idx_section_class_id   ON school_sections (class_id);
CREATE INDEX idx_section_teacher_id ON school_sections (class_teacher_id);
CREATE INDEX idx_section_active     ON school_sections (active);

COMMENT ON TABLE  school_sections                   IS 'Divisions within a class (A, B, C or Morning/Afternoon)';
COMMENT ON COLUMN school_sections.class_teacher_id  IS 'UUID of SystemUser — soft reference (no FK) to avoid cross-module dependency';
COMMENT ON COLUMN school_sections.shift_id          IS 'NULL = inherits default shift from academic year';


-- -----------------------------------------------------------------------------
-- 4. class_subjects
--    Which subjects are taught in which class, with per-class mark overrides
-- -----------------------------------------------------------------------------
CREATE TABLE class_subjects (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMP       NOT NULL,
    updated_at                  TIMESTAMP,
    created_by                  UUID,
    updated_by                  UUID,
    is_deleted                  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMP,

    class_id                    UUID            NOT NULL,
    subject_id                  UUID            NOT NULL,
    offering_type               VARCHAR(15)     NOT NULL DEFAULT 'COMPULSORY',
    theory_periods_per_week     INTEGER,
    practical_periods_per_week  INTEGER,
    max_theory_marks            INTEGER,
    max_practical_marks         INTEGER,

    CONSTRAINT pk_class_subjects        PRIMARY KEY (id),
    CONSTRAINT uq_class_subject         UNIQUE (class_id, subject_id),
    CONSTRAINT fk_cs_class              FOREIGN KEY (class_id)
                                            REFERENCES school_classes (id)
                                            ON DELETE CASCADE,
    CONSTRAINT fk_cs_subject            FOREIGN KEY (subject_id)
                                            REFERENCES subjects (id)
                                            ON DELETE RESTRICT,
    CONSTRAINT chk_offering_type        CHECK (offering_type IN ('COMPULSORY','OPTIONAL'))
);

CREATE INDEX idx_cs_class_id    ON class_subjects (class_id);
CREATE INDEX idx_cs_subject_id  ON class_subjects (subject_id);

COMMENT ON TABLE  class_subjects                        IS 'Subject-to-class assignments with optional per-class mark overrides';
COMMENT ON COLUMN class_subjects.offering_type          IS 'COMPULSORY = all students, OPTIONAL = student-choice elective';
COMMENT ON COLUMN class_subjects.max_theory_marks       IS 'NULL = inherit from subjects table';


-- -----------------------------------------------------------------------------
-- 5. section_subject_teachers
--    Maps which teacher teaches which subject in which section
-- -----------------------------------------------------------------------------
CREATE TABLE section_subject_teachers (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,

    section_id          UUID            NOT NULL,
    class_subject_id    UUID            NOT NULL,
    teacher_id          UUID            NOT NULL,   -- soft ref to system_users
    assignment_type     VARCHAR(15)     NOT NULL DEFAULT 'THEORY',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_sst                   PRIMARY KEY (id),
    CONSTRAINT uq_section_subject       UNIQUE (section_id, class_subject_id),
    CONSTRAINT fk_sst_section           FOREIGN KEY (section_id)
                                            REFERENCES school_sections (id)
                                            ON DELETE CASCADE,
    CONSTRAINT fk_sst_class_subject     FOREIGN KEY (class_subject_id)
                                            REFERENCES class_subjects (id)
                                            ON DELETE CASCADE,
    CONSTRAINT chk_assignment_type      CHECK (assignment_type IN ('THEORY','PRACTICAL'))
);

CREATE INDEX idx_sst_section_id     ON section_subject_teachers (section_id);
CREATE INDEX idx_sst_teacher_id     ON section_subject_teachers (teacher_id);
CREATE INDEX idx_sst_class_subj_id  ON section_subject_teachers (class_subject_id);

COMMENT ON TABLE  section_subject_teachers              IS 'Teacher → subject → section assignment; drives timetable, attendance, mark entry';
COMMENT ON COLUMN section_subject_teachers.teacher_id   IS 'UUID of SystemUser — soft reference (no FK)';
COMMENT ON COLUMN section_subject_teachers.assignment_type IS 'THEORY = main class teacher; PRACTICAL = lab/demo teacher';

-- =============================================================================
-- END OF MIGRATION
-- =============================================================================
