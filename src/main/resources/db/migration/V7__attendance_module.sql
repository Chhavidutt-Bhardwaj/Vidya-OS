-- =============================================================================
-- V7__attendance_module.sql
-- Student attendance + Staff attendance
--
-- Depends on: V6 (students), V2 (system_users)
-- =============================================================================

-- =============================================================================
-- student_attendance
-- =============================================================================
CREATE TABLE student_attendance (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP,
    created_by        UUID,
    updated_by        UUID,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP,

    school_id         UUID            NOT NULL,
    academic_year_id  UUID            NOT NULL,
    section_id        UUID            NOT NULL,
    student_id        UUID            NOT NULL,
    teacher_id        UUID            NOT NULL,
    attendance_date   DATE            NOT NULL,
    period_number     INTEGER         NOT NULL DEFAULT 0, -- 0=daily, 1-8=period-wise
    status            VARCHAR(15)     NOT NULL
                      CHECK (status IN (
                          'PRESENT', 'ABSENT', 'LATE', 'HALF_DAY',
                          'ON_LEAVE', 'MEDICAL_LEAVE'
                      )),
    remarks           VARCHAR(200),
    marked_at         TIME,
    is_corrected      BOOLEAN         NOT NULL DEFAULT FALSE,
    correction_reason VARCHAR(500),

    CONSTRAINT pk_student_attendance  PRIMARY KEY (id),
    CONSTRAINT fk_sa_school           FOREIGN KEY (school_id)        REFERENCES schools(id),
    CONSTRAINT fk_sa_year             FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_sa_section          FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_sa_student          FOREIGN KEY (student_id)       REFERENCES students(id),
    CONSTRAINT uq_student_att_date    UNIQUE (student_id, attendance_date, period_number)
);

CREATE INDEX idx_sa_section_date  ON student_attendance(section_id, attendance_date);
CREATE INDEX idx_sa_student_year  ON student_attendance(student_id, academic_year_id);
CREATE INDEX idx_sa_date          ON student_attendance(attendance_date);
CREATE INDEX idx_sa_status        ON student_attendance(status);
CREATE INDEX idx_sa_school_date   ON student_attendance(school_id, attendance_date);

COMMENT ON TABLE  student_attendance               IS 'Daily or period-wise student attendance records';
COMMENT ON COLUMN student_attendance.period_number IS '0 = daily attendance; 1-8 = specific period number';

-- =============================================================================
-- staff_attendance
-- =============================================================================
CREATE TABLE staff_attendance (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP,
    created_by        UUID,
    updated_by        UUID,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP,

    school_id         UUID            NOT NULL,
    academic_year_id  UUID            NOT NULL,
    staff_id          UUID            NOT NULL,
    marked_by         UUID            NOT NULL,
    attendance_date   DATE            NOT NULL,
    status            VARCHAR(20)     NOT NULL
                      CHECK (status IN (
                          'PRESENT', 'ABSENT', 'LATE', 'HALF_DAY',
                          'ON_LEAVE', 'MEDICAL_LEAVE', 'WORK_FROM_HOME'
                      )),
    check_in_time     TIME,
    check_out_time    TIME,
    remarks           VARCHAR(300),
    leave_type        VARCHAR(30),

    CONSTRAINT pk_staff_attendance PRIMARY KEY (id),
    CONSTRAINT fk_staff_att_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT uq_staff_att_date   UNIQUE (staff_id, attendance_date)
);

CREATE INDEX idx_staff_att_staff_date ON staff_attendance(staff_id, attendance_date);
CREATE INDEX idx_staff_att_school     ON staff_attendance(school_id, attendance_date);
CREATE INDEX idx_staff_att_year       ON staff_attendance(academic_year_id);

-- =============================================================================
-- attendance_leave_requests
-- =============================================================================
CREATE TABLE attendance_leave_requests (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP,
    created_by        UUID,
    updated_by        UUID,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP,

    school_id         UUID            NOT NULL,
    student_id        UUID,                               -- null for staff leave
    staff_id          UUID,                               -- null for student leave
    from_date         DATE            NOT NULL,
    to_date           DATE            NOT NULL,
    leave_type        VARCHAR(30)     NOT NULL,
    reason            TEXT            NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    approved_by       UUID,
    approved_on       TIMESTAMP,
    rejection_reason  VARCHAR(500),

    CONSTRAINT pk_leave_requests PRIMARY KEY (id),
    CONSTRAINT fk_lr_school      FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT chk_lr_actor      CHECK (
        (student_id IS NOT NULL AND staff_id IS NULL) OR
        (student_id IS NULL AND staff_id IS NOT NULL)
    )
);

CREATE INDEX idx_lr_student_id ON attendance_leave_requests(student_id);
CREATE INDEX idx_lr_staff_id   ON attendance_leave_requests(staff_id);
CREATE INDEX idx_lr_dates      ON attendance_leave_requests(from_date, to_date);
CREATE INDEX idx_lr_status     ON attendance_leave_requests(status);

-- =============================================================================
-- END V7
-- =============================================================================
