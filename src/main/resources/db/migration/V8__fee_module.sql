-- =============================================================================
-- V8__fee_module.sql
-- Fee instalments, discounts, payments, receipts
--
-- Depends on: V6 (students), V4 (fee_structure_heads)
-- =============================================================================

-- =============================================================================
-- fee_discounts
-- =============================================================================
CREATE TABLE fee_discounts (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP,
    created_by        UUID,
    updated_by        UUID,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP,

    school_id         UUID            NOT NULL,
    student_id        UUID            NOT NULL,
    academic_year_id  UUID            NOT NULL,
    discount_type     VARCHAR(30)     NOT NULL
                      CHECK (discount_type IN (
                          'SIBLING', 'SCHOLARSHIP', 'MERIT', 'STAFF_WARD',
                          'CUSTOM', 'EARLY_BIRD', 'BULK'
                      )),
    discount_name     VARCHAR(100)    NOT NULL,
    discount_value    NUMERIC(10,2)   NOT NULL,
    discount_mode     VARCHAR(15)     NOT NULL CHECK (discount_mode IN ('PERCENTAGE', 'FLAT')),
    max_cap           NUMERIC(10,2),
    valid_from        DATE            NOT NULL,
    valid_to          DATE,
    approved_by       UUID            NOT NULL,
    remarks           VARCHAR(500),
    active            BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_fee_discounts     PRIMARY KEY (id),
    CONSTRAINT fk_fd_school         FOREIGN KEY (school_id)        REFERENCES schools(id),
    CONSTRAINT fk_fd_student        FOREIGN KEY (student_id)       REFERENCES students(id),
    CONSTRAINT fk_fd_year           FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT chk_discount_value   CHECK (discount_value > 0)
);

CREATE INDEX idx_fd_student_id  ON fee_discounts(student_id, academic_year_id);
CREATE INDEX idx_fd_active      ON fee_discounts(active, valid_from, valid_to);

-- =============================================================================
-- fee_instalments
-- =============================================================================
CREATE TABLE fee_instalments (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP,
    created_by              UUID,
    updated_by              UUID,
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP,

    school_id               UUID            NOT NULL,
    student_id              UUID            NOT NULL,
    academic_year_id        UUID            NOT NULL,
    fee_structure_head_id   UUID            NOT NULL,
    instalment_number       INTEGER         NOT NULL,
    base_amount             NUMERIC(12,2)   NOT NULL,
    discount_amount         NUMERIC(12,2)   NOT NULL DEFAULT 0,
    late_fee_amount         NUMERIC(12,2)   NOT NULL DEFAULT 0,
    gst_amount              NUMERIC(12,2)   NOT NULL DEFAULT 0,
    net_amount              NUMERIC(12,2)   NOT NULL,
    amount_paid             NUMERIC(12,2)   NOT NULL DEFAULT 0,
    due_date                DATE            NOT NULL,
    paid_date               DATE,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN (
                                'PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'WAIVED', 'CANCELLED'
                            )),
    receipt_no              VARCHAR(30),
    waiver_reason           VARCHAR(500),
    waived_by               UUID,

    CONSTRAINT pk_fee_instalments  PRIMARY KEY (id),
    CONSTRAINT fk_fi_school        FOREIGN KEY (school_id)              REFERENCES schools(id),
    CONSTRAINT fk_fi_student       FOREIGN KEY (student_id)             REFERENCES students(id),
    CONSTRAINT fk_fi_year          FOREIGN KEY (academic_year_id)       REFERENCES academic_years(id),
    CONSTRAINT fk_fi_head          FOREIGN KEY (fee_structure_head_id)  REFERENCES fee_structure_heads(id),
    CONSTRAINT chk_fi_amounts      CHECK (
        base_amount >= 0 AND discount_amount >= 0 AND net_amount >= 0 AND amount_paid >= 0
    )
);

CREATE INDEX idx_fi_student_id    ON fee_instalments(student_id, academic_year_id);
CREATE INDEX idx_fi_due_date      ON fee_instalments(due_date);
CREATE INDEX idx_fi_status        ON fee_instalments(status);
CREATE INDEX idx_fi_school_year   ON fee_instalments(school_id, academic_year_id);

-- Partial index for overdue detection
CREATE INDEX idx_fi_overdue
    ON fee_instalments(school_id, due_date)
    WHERE status IN ('PENDING', 'PARTIAL');

COMMENT ON TABLE  fee_instalments              IS 'Individual fee instalments due per student';
COMMENT ON COLUMN fee_instalments.net_amount   IS 'base_amount - discount_amount + late_fee_amount + gst_amount';
COMMENT ON COLUMN fee_instalments.amount_paid  IS 'Running total collected so far; balance = net_amount - amount_paid';

-- =============================================================================
-- fee_payments  (receipt header)
-- =============================================================================
CREATE TABLE fee_payments (
    id               UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at       TIMESTAMP       NOT NULL,
    updated_at       TIMESTAMP,
    created_by       UUID,
    updated_by       UUID,
    is_deleted       BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP,

    school_id        UUID            NOT NULL,
    student_id       UUID            NOT NULL,
    academic_year_id UUID            NOT NULL,
    receipt_no       VARCHAR(30)     NOT NULL,
    payment_date     DATE            NOT NULL,
    total_amount     NUMERIC(12,2)   NOT NULL,
    payment_mode     VARCHAR(20)     NOT NULL
                     CHECK (payment_mode IN (
                         'CASH', 'ONLINE', 'CHEQUE', 'DD', 'UPI', 'NEFT', 'RTGS', 'CARD'
                     )),
    transaction_ref  VARCHAR(100),
    bank_name        VARCHAR(100),
    cheque_no        VARCHAR(50),
    collected_by     UUID            NOT NULL,
    remarks          VARCHAR(500),
    is_refunded      BOOLEAN         NOT NULL DEFAULT FALSE,
    refunded_amount  NUMERIC(12,2),
    refund_date      DATE,
    refund_reason    VARCHAR(500),

    CONSTRAINT pk_fee_payments   PRIMARY KEY (id),
    CONSTRAINT fk_fp_school      FOREIGN KEY (school_id)        REFERENCES schools(id),
    CONSTRAINT fk_fp_student     FOREIGN KEY (student_id)       REFERENCES students(id),
    CONSTRAINT fk_fp_year        FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT uq_fp_receipt_no  UNIQUE (school_id, receipt_no)
);

CREATE INDEX idx_fp_student_id  ON fee_payments(student_id, academic_year_id);
CREATE INDEX idx_fp_receipt_no  ON fee_payments(school_id, receipt_no);
CREATE INDEX idx_fp_date        ON fee_payments(payment_date);
CREATE INDEX idx_fp_mode        ON fee_payments(payment_mode);

-- =============================================================================
-- fee_payment_items  (receipt line items)
-- =============================================================================
CREATE TABLE fee_payment_items (
    id               UUID            NOT NULL DEFAULT gen_random_uuid(),
    created_at       TIMESTAMP       NOT NULL,
    updated_at       TIMESTAMP,
    created_by       UUID,
    updated_by       UUID,
    is_deleted       BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP,

    payment_id       UUID            NOT NULL,
    instalment_id    UUID            NOT NULL,
    amount           NUMERIC(12,2)   NOT NULL,

    CONSTRAINT pk_fee_payment_items PRIMARY KEY (id),
    CONSTRAINT fk_fpi_payment       FOREIGN KEY (payment_id)    REFERENCES fee_payments(id)    ON DELETE CASCADE,
    CONSTRAINT fk_fpi_instalment    FOREIGN KEY (instalment_id) REFERENCES fee_instalments(id)
);

CREATE INDEX idx_fpi_payment_id    ON fee_payment_items(payment_id);
CREATE INDEX idx_fpi_instalment_id ON fee_payment_items(instalment_id);

-- =============================================================================
-- receipt_number_sequence  (school-specific receipt counters)
-- =============================================================================
CREATE TABLE receipt_number_sequences (
    school_id    UUID        NOT NULL,
    year_prefix  VARCHAR(10) NOT NULL,
    last_number  BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_receipt_seq PRIMARY KEY (school_id, year_prefix)
);

-- =============================================================================
-- END V8
-- =============================================================================
