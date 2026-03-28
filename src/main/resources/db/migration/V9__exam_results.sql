-- =============================================================================
-- V9__exam_results.sql
-- Exam results, report cards, exam marks entry
--
-- Depends on: V6 (students), V5 (exam_schedules)
-- =============================================================================

-- =============================================================================
-- exam_results
-- =============================================================================
CREATE TABLE exam_results (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMP       NOT NULL,
    updated_at                  TIMESTAMP,
    created_by                  UUID,
    updated_by                  UUID,
    is_deleted                  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMP,

    school_id                   UUID            NOT NULL,
    academic_year_id            UUID            NOT NULL,
    exam_schedule_id            UUID            NOT NULL,
    student_id                  UUID            NOT NULL,
    section_id                  UUID            NOT NULL,
    subject_code                VARCHAR(30)     NOT NULL,
    entered_by                  UUID            NOT NULL,
    theory_marks_obtained       NUMERIC(6,2),
    practical_marks_obtained    NUMERIC(6,2),
    total_marks_obtained        NUMERIC(6,2)
                                GENERATED ALWAYS AS (
                                    COALESCE(theory_marks_obtained, 0) +
                                    COALESCE(practical_marks_obtained, 0)
                                ) STORED,
    max_marks                   NUMERIC(6,2)    NOT NULL,
    percentage                  NUMERIC(5,2),
    grade                       VARCHAR(5),
    grade_point                 NUMERIC(4,2),
    result_status               VARCHAR(15)     NOT NULL DEFAULT 'PENDING'
                                CHECK (result_status IN (
                                    'PENDING', 'PASS', 'FAIL',
                                    'ABSENT', 'EXEMPTED', 'WITHHELD'
                                )),
    remarks                     VARCHAR(500),
    is_absent                   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_published                BOOLEAN         NOT NULL DEFAULT FALSE,
    published_at                TIMESTAMP,
    published_by                UUID,

    CONSTRAINT pk_exam_results    PRIMARY KEY (id),
    CONSTRAINT fk_er_school       FOREIGN KEY (school_id)       REFERENCES schools(id),
    CONSTRAINT fk_er_year         FOREIGN KEY (academic_year_id)REFERENCES academic_years(id),
    CONSTRAINT fk_er_schedule     FOREIGN KEY (exam_schedule_id)REFERENCES exam_schedules(id),
    CONSTRAINT fk_er_student      FOREIGN KEY (student_id)      REFERENCES students(id),
    CONSTRAINT fk_er_section      FOREIGN KEY (section_id)      REFERENCES school_sections(id),
    CONSTRAINT uq_exam_result     UNIQUE (exam_schedule_id, student_id, subject_code),
    CONSTRAINT chk_er_marks       CHECK (
        (theory_marks_obtained IS NULL OR theory_marks_obtained >= 0) AND
        (practical_marks_obtained IS NULL OR practical_marks_obtained >= 0)
    )
);

CREATE INDEX idx_er_exam_schedule_id ON exam_results(exam_schedule_id);
CREATE INDEX idx_er_student_id       ON exam_results(student_id, academic_year_id);
CREATE INDEX idx_er_section_id       ON exam_results(section_id);
CREATE INDEX idx_er_published        ON exam_results(is_published);
CREATE INDEX idx_er_subject          ON exam_results(school_id, subject_code, academic_year_id);

COMMENT ON TABLE exam_results IS 'Per-student per-subject marks for each exam schedule';

-- =============================================================================
-- report_cards  (generated report card metadata)
-- =============================================================================
CREATE TABLE report_cards (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,

    school_id           UUID            NOT NULL,
    academic_year_id    UUID            NOT NULL,
    term_id             UUID,                        -- NULL = annual report card
    student_id          UUID            NOT NULL,
    section_id          UUID            NOT NULL,
    total_marks_obtained NUMERIC(8,2),
    total_max_marks     NUMERIC(8,2),
    overall_percentage  NUMERIC(5,2),
    overall_grade       VARCHAR(5),
    overall_grade_point NUMERIC(4,2),
    class_rank          INTEGER,
    attendance_pct      NUMERIC(5,2),
    days_present        INTEGER,
    total_working_days  INTEGER,
    result              VARCHAR(20),                 -- 'PASS' | 'FAIL' | 'PROMOTED' | 'DETAINED'
    principal_remarks   TEXT,
    teacher_remarks     TEXT,
    is_published        BOOLEAN         NOT NULL DEFAULT FALSE,
    published_at        TIMESTAMP,
    pdf_url             VARCHAR(512),

    CONSTRAINT pk_report_cards       PRIMARY KEY (id),
    CONSTRAINT fk_rc_school          FOREIGN KEY (school_id)        REFERENCES schools(id),
    CONSTRAINT fk_rc_year            FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_rc_student         FOREIGN KEY (student_id)       REFERENCES students(id),
    CONSTRAINT uq_report_card        UNIQUE (student_id, academic_year_id, term_id)
);

CREATE INDEX idx_rc_student_year  ON report_cards(student_id, academic_year_id);
CREATE INDEX idx_rc_section       ON report_cards(section_id, academic_year_id);
CREATE INDEX idx_rc_published     ON report_cards(is_published);

-- =============================================================================
-- admission_number_sequences  (school-specific counters)
-- =============================================================================
CREATE TABLE admission_number_sequences (
    school_id    UUID        NOT NULL,
    year_prefix  VARCHAR(10) NOT NULL,
    last_number  BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_admission_seq PRIMARY KEY (school_id, year_prefix)
);

-- =============================================================================
-- END V9
-- =============================================================================
