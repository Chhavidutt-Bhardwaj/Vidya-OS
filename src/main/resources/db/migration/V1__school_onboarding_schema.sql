-- =============================================================================
-- V1__school_onboarding_schema.sql
-- School Onboarding — complete schema
--
-- Table creation order respects FK dependencies:
--   1. school_chains
--   2. schools
--   3. school_basic_info
--   4. school_addresses
--   5. school_settings
--   6. school_contacts
--   7. school_documents
--   8. school_grade_ranges
--   9. school_facilities
--  10. school_social_links
--  11. school_onboarding_audit
--  12. academic_years
--  13. school_terms
--  14. school_shifts
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. school_chains
--    Groups / franchises that own multiple school branches.
--    Standalone schools do NOT require a chain row.
-- -----------------------------------------------------------------------------
CREATE TABLE school_chains (
    -- BaseEntity columns
                               id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                               created_at          TIMESTAMP       NOT NULL,
                               updated_at          TIMESTAMP,
                               created_by          UUID,
                               updated_by          UUID,
                               is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                               deleted_at          TIMESTAMP,

    -- Domain columns
                               name                VARCHAR(255)    NOT NULL,
                               chain_code          VARCHAR(30)     NOT NULL,
                               description         VARCHAR(1000),
                               website             VARCHAR(255),
                               active              BOOLEAN         NOT NULL DEFAULT TRUE,

                               CONSTRAINT pk_school_chains PRIMARY KEY (id),
                               CONSTRAINT uq_chain_code    UNIQUE (chain_code)
);

CREATE INDEX idx_chain_active    ON school_chains (active);
CREATE INDEX idx_chain_deleted   ON school_chains (is_deleted);

COMMENT ON TABLE  school_chains              IS 'School groups / franchises (DPS Group, Ryan International, etc.)';
COMMENT ON COLUMN school_chains.chain_code   IS 'Short unique code like DPS, RYAN — used in branch prefixes';
COMMENT ON COLUMN school_chains.is_deleted   IS 'Soft-delete flag — never hard-delete chains';


-- -----------------------------------------------------------------------------
-- 2. schools
--    One row = one physical campus / branch.
--    chain_id NULL  → standalone school
--    chain_id SET   → branch of that chain
--    is_headquarter → the admin/HQ branch of a chain
-- -----------------------------------------------------------------------------
CREATE TABLE schools (
    -- BaseEntity columns
                         id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
                         created_at              TIMESTAMP       NOT NULL,
                         updated_at              TIMESTAMP,
                         created_by              UUID,
                         updated_by              UUID,
                         is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
                         deleted_at              TIMESTAMP,

    -- Chain / Branch
                         chain_id                UUID,
                         branch_code             VARCHAR(30),
                         branch_name             VARCHAR(100),
                         is_headquarter          BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Basic identity
                         name                    VARCHAR(255)    NOT NULL,
                         type                    VARCHAR(30)     NOT NULL,
                         board                   VARCHAR(30),
                         medium                  VARCHAR(30),
                         udise_code              VARCHAR(20),
                         affiliation_no          VARCHAR(50),
                         dise_code               VARCHAR(20),

    -- Plan & counters
                         plan                    VARCHAR(30)     NOT NULL DEFAULT 'STARTER',
                         student_count           INTEGER         NOT NULL DEFAULT 0,
                         active                  BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Onboarding state machine
                         onboarding_step         VARCHAR(30)     NOT NULL DEFAULT 'BASIC_INFO',
                         onboarding_complete     BOOLEAN         NOT NULL DEFAULT FALSE,

                         CONSTRAINT pk_schools               PRIMARY KEY (id),
                         CONSTRAINT uq_school_udise_code     UNIQUE (udise_code),
                         CONSTRAINT fk_school_chain          FOREIGN KEY (chain_id)
                             REFERENCES school_chains (id)
                             ON DELETE RESTRICT,

                         CONSTRAINT chk_school_type          CHECK (type IN (
                                                                             'PRE_PRIMARY','PRIMARY','MIDDLE','SECONDARY',
                                                                             'SENIOR_SECONDARY','K12','JUNIOR_COLLEGE','SPECIAL_NEEDS'
                             )),
                         CONSTRAINT chk_school_board         CHECK (board IS NULL OR board IN (
                                                                                               'CBSE','ICSE','IB','IGCSE','STATE_BOARD','NIOS','OTHER'
                             )),
                         CONSTRAINT chk_school_plan          CHECK (plan IN (
                                                                             'STARTER','BASIC','PRO','ENTERPRISE'
                             )),
                         CONSTRAINT chk_onboarding_step      CHECK (onboarding_step IN (
                                                                                        'BASIC_INFO','CONTACT','ADDRESS','ACADEMIC','DOCUMENTS','COMPLETE'
                             )),
                         CONSTRAINT chk_student_count        CHECK (student_count >= 0)
);

-- Standard lookup indexes
CREATE INDEX idx_school_chain_id        ON schools (chain_id);
CREATE INDEX idx_school_active          ON schools (active);
CREATE INDEX idx_school_type            ON schools (type);
CREATE INDEX idx_school_plan            ON schools (plan);
CREATE INDEX idx_school_board           ON schools (board);
CREATE INDEX idx_school_udise           ON schools (udise_code);
CREATE INDEX idx_school_onboard_step    ON schools (onboarding_step);
CREATE INDEX idx_school_deleted         ON schools (is_deleted);

-- Partial unique index: only one HQ branch per chain (enforced at DB level)
CREATE UNIQUE INDEX uq_chain_headquarter
    ON schools (chain_id)
    WHERE is_headquarter = TRUE AND is_deleted = FALSE;

-- Partial unique index: branch_code unique within a chain
CREATE UNIQUE INDEX uq_chain_branch_code
    ON schools (chain_id, branch_code)
    WHERE branch_code IS NOT NULL AND is_deleted = FALSE;

COMMENT ON TABLE  schools                   IS 'One row per physical school campus or branch';
COMMENT ON COLUMN schools.chain_id          IS 'NULL for standalone schools; SET for chain branches';
COMMENT ON COLUMN schools.is_headquarter    IS 'Only one branch per chain may be TRUE — enforced by uq_chain_headquarter';
COMMENT ON COLUMN schools.onboarding_step   IS 'Current onboarding step: BASIC_INFO→CONTACT→ADDRESS→ACADEMIC→DOCUMENTS→COMPLETE';
COMMENT ON COLUMN schools.udise_code        IS 'Government UDISE+ code — unique per physical campus';


-- -----------------------------------------------------------------------------
-- 3. school_basic_info
--    Extended, rarely-loaded info separated from the hot schools row.
--    Principal, logo, trust, founding date, etc.
-- -----------------------------------------------------------------------------
CREATE TABLE school_basic_info (
    -- BaseEntity columns
                                   id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
                                   created_at              TIMESTAMP       NOT NULL,
                                   updated_at              TIMESTAMP,
                                   created_by              UUID,
                                   updated_by              UUID,
                                   is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
                                   deleted_at              TIMESTAMP,

    -- FK
                                   school_id               UUID            NOT NULL,

    -- Principal / head
                                   tagline                 VARCHAR(500),
                                   description             TEXT,
                                   established_year        INTEGER,
                                   founded_on              DATE,
                                   principal_name          VARCHAR(150),
                                   principal_designation   VARCHAR(100),

    -- Contact / web
                                   official_email          VARCHAR(255),
                                   website                 VARCHAR(255),
                                   phone_primary           VARCHAR(20),
                                   phone_secondary         VARCHAR(20),

    -- Assets
                                   logo_url                VARCHAR(512),
                                   cover_image_url         VARCHAR(512),

    -- Legal / admin
                                   registration_number     VARCHAR(100),
                                   trust_name              VARCHAR(255),
                                   management_type         VARCHAR(50),
                                   is_co_ed                BOOLEAN         NOT NULL DEFAULT TRUE,
                                   is_residential          BOOLEAN         NOT NULL DEFAULT FALSE,

                                   CONSTRAINT pk_school_basic_info     PRIMARY KEY (id),
                                   CONSTRAINT uq_basic_info_school     UNIQUE (school_id),
                                   CONSTRAINT fk_basic_info_school     FOREIGN KEY (school_id)
                                       REFERENCES schools (id)
                                       ON DELETE CASCADE,
                                   CONSTRAINT chk_management_type      CHECK (management_type IS NULL OR management_type IN (
                                                                                                                             'GOVERNMENT','PRIVATE','AIDED','AUTONOMOUS','OTHER'
                                       )),
                                   CONSTRAINT chk_established_year     CHECK (established_year IS NULL OR established_year BETWEEN 1800 AND 2100)
);

CREATE INDEX idx_basic_info_school_id   ON school_basic_info (school_id);

COMMENT ON TABLE  school_basic_info                  IS 'Extended school info — lazy loaded, not needed on every request';
COMMENT ON COLUMN school_basic_info.management_type  IS 'GOVERNMENT, PRIVATE, AIDED, AUTONOMOUS, OTHER';
COMMENT ON COLUMN school_basic_info.is_residential   IS 'TRUE if the school has a boarding / hostel facility';


-- -----------------------------------------------------------------------------
-- 4. school_addresses
--    Physical location of one campus.
--    Inherits all AddressEntity fields + school-specific extras.
-- -----------------------------------------------------------------------------
CREATE TABLE school_addresses (
    -- BaseEntity columns
                                  id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                  created_at          TIMESTAMP       NOT NULL,
                                  updated_at          TIMESTAMP,
                                  created_by          UUID,
                                  updated_by          UUID,
                                  is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                                  deleted_at          TIMESTAMP,

    -- FK
                                  school_id           UUID            NOT NULL,

    -- AddressEntity columns
                                  address_line1       VARCHAR(255),
                                  address_line2       VARCHAR(255),
                                  landmark            VARCHAR(255),
                                  city                VARCHAR(100),
                                  district            VARCHAR(100),
                                  state               VARCHAR(100),
                                  pincode             VARCHAR(10),
                                  latitude            NUMERIC(10, 7),
                                  longitude           NUMERIC(10, 7),

    -- School-specific extras
                                  directions          TEXT,
                                  map_link            VARCHAR(512),
                                  google_place_id     VARCHAR(100),

                                  CONSTRAINT pk_school_addresses      PRIMARY KEY (id),
                                  CONSTRAINT uq_address_school        UNIQUE (school_id),
                                  CONSTRAINT fk_address_school        FOREIGN KEY (school_id)
                                      REFERENCES schools (id)
                                      ON DELETE CASCADE,
                                  CONSTRAINT chk_latitude             CHECK (latitude  IS NULL OR latitude  BETWEEN -90  AND 90),
                                  CONSTRAINT chk_longitude            CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_school_addr_school_id  ON school_addresses (school_id);
CREATE INDEX idx_school_addr_pincode    ON school_addresses (pincode);
CREATE INDEX idx_school_addr_city       ON school_addresses (city);
CREATE INDEX idx_school_addr_state      ON school_addresses (state);

COMMENT ON TABLE  school_addresses              IS 'Physical address of one school campus (1-to-1 with schools)';
COMMENT ON COLUMN school_addresses.directions   IS 'Human-readable directions to the school gate';
COMMENT ON COLUMN school_addresses.latitude     IS 'WGS-84 decimal degrees, precision 10,7';


-- -----------------------------------------------------------------------------
-- 5. school_settings
--    Per-school configuration: locale, fee, attendance, notifications, branding.
-- -----------------------------------------------------------------------------
CREATE TABLE school_settings (
    -- BaseEntity columns
                                 id                              UUID            NOT NULL DEFAULT gen_random_uuid(),
                                 created_at                      TIMESTAMP       NOT NULL,
                                 updated_at                      TIMESTAMP,
                                 created_by                      UUID,
                                 updated_by                      UUID,
                                 is_deleted                      BOOLEAN         NOT NULL DEFAULT FALSE,
                                 deleted_at                      TIMESTAMP,

    -- FK
                                 school_id                       UUID            NOT NULL,

    -- Locale & regional
                                 locale                          VARCHAR(10)     DEFAULT 'en-IN',
                                 timezone                        VARCHAR(50)     DEFAULT 'Asia/Kolkata',
                                 currency                        VARCHAR(5)      DEFAULT 'INR',
                                 date_format                     VARCHAR(20)     DEFAULT 'DD/MM/YYYY',

    -- Academic calendar
                                 academic_year_start_month       INTEGER         NOT NULL DEFAULT 4,

    -- Attendance
                                 min_attendance_pct              INTEGER         NOT NULL DEFAULT 75,
                                 saturday_working                BOOLEAN         NOT NULL DEFAULT TRUE,
                                 alternate_saturday_off          BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Fee
                                 gst_applicable                  BOOLEAN         NOT NULL DEFAULT FALSE,
                                 gstin                           VARCHAR(20),
                                 fee_due_grace_days              INTEGER         NOT NULL DEFAULT 5,

    -- Notifications
                                 sms_enabled                     BOOLEAN         NOT NULL DEFAULT FALSE,
                                 whatsapp_enabled                BOOLEAN         NOT NULL DEFAULT FALSE,
                                 email_notifications_enabled     BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Branding
                                 brand_color_primary             VARCHAR(10),
                                 brand_color_secondary           VARCHAR(10),

                                 CONSTRAINT pk_school_settings               PRIMARY KEY (id),
                                 CONSTRAINT uq_settings_school               UNIQUE (school_id),
                                 CONSTRAINT fk_settings_school               FOREIGN KEY (school_id)
                                     REFERENCES schools (id)
                                     ON DELETE CASCADE,
                                 CONSTRAINT chk_academic_start_month         CHECK (academic_year_start_month BETWEEN 1 AND 12),
                                 CONSTRAINT chk_min_attendance               CHECK (min_attendance_pct BETWEEN 0 AND 100),
                                 CONSTRAINT chk_fee_grace_days               CHECK (fee_due_grace_days >= 0),
                                 CONSTRAINT chk_gstin_format                 CHECK (
                                     gstin IS NULL OR gstin ~ '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$'
                                     )
);

CREATE INDEX idx_settings_school_id ON school_settings (school_id);

COMMENT ON TABLE  school_settings                        IS 'Per-school configuration — locale, fees, attendance rules, notifications';
COMMENT ON COLUMN school_settings.academic_year_start_month IS '1=Jan … 12=Dec; default 4 (April) for Indian academic year';
COMMENT ON COLUMN school_settings.gstin                  IS 'Indian GST registration number — validated by check constraint';


-- -----------------------------------------------------------------------------
-- 6. school_contacts
--    Named contact persons for a school (principal, admin, accounts, etc.).
--    A school can have multiple contacts; one is flagged is_primary.
-- -----------------------------------------------------------------------------
CREATE TABLE school_contacts (
    -- BaseEntity columns
                                 id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
                                 created_at              TIMESTAMP       NOT NULL,
                                 updated_at              TIMESTAMP,
                                 created_by              UUID,
                                 updated_by              UUID,
                                 is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
                                 deleted_at              TIMESTAMP,

    -- FK
                                 school_id               UUID            NOT NULL,

    -- Contact details
                                 contact_type            VARCHAR(30)     NOT NULL,
                                 full_name               VARCHAR(150)    NOT NULL,
                                 designation             VARCHAR(100),
                                 email                   VARCHAR(255),
                                 phone                   VARCHAR(20)     NOT NULL,
                                 phone_alternate         VARCHAR(20),
                                 is_primary              BOOLEAN         NOT NULL DEFAULT FALSE,
                                 receive_notifications   BOOLEAN         NOT NULL DEFAULT TRUE,

                                 CONSTRAINT pk_school_contacts       PRIMARY KEY (id),
                                 CONSTRAINT fk_contact_school        FOREIGN KEY (school_id)
                                     REFERENCES schools (id)
                                     ON DELETE CASCADE,
                                 CONSTRAINT chk_contact_type         CHECK (contact_type IN (
                                                                                             'PRINCIPAL','VICE_PRINCIPAL','ADMIN','ACCOUNTS',
                                                                                             'ADMISSION','TRANSPORT','SUPPORT','OTHER'
                                     ))
);

CREATE INDEX idx_contact_school_id  ON school_contacts (school_id);
CREATE INDEX idx_contact_type       ON school_contacts (contact_type);
CREATE INDEX idx_contact_primary    ON school_contacts (is_primary);

-- Only one primary contact per school (partial unique index)
CREATE UNIQUE INDEX uq_school_primary_contact
    ON school_contacts (school_id)
    WHERE is_primary = TRUE AND is_deleted = FALSE;

COMMENT ON TABLE  school_contacts               IS 'Contact persons for a school — principal, admin, accounts, etc.';
COMMENT ON COLUMN school_contacts.is_primary    IS 'Only one contact per school can be primary — enforced by uq_school_primary_contact';


-- -----------------------------------------------------------------------------
-- 7. school_documents
--    Files uploaded during or after onboarding (certs, logo, NOC, etc.).
-- -----------------------------------------------------------------------------
CREATE TABLE school_documents (
    -- BaseEntity columns
                                  id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                  created_at          TIMESTAMP       NOT NULL,
                                  updated_at          TIMESTAMP,
                                  created_by          UUID,
                                  updated_by          UUID,
                                  is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                                  deleted_at          TIMESTAMP,

    -- FK
                                  school_id           UUID            NOT NULL,

    -- Document metadata
                                  document_type       VARCHAR(50)     NOT NULL,
                                  document_name       VARCHAR(255)    NOT NULL,
                                  file_url            VARCHAR(512)    NOT NULL,
                                  mime_type           VARCHAR(100),
                                  file_size_bytes     BIGINT,
                                  original_file_name  VARCHAR(255),
                                  verified            BOOLEAN         NOT NULL DEFAULT FALSE,
                                  expires_on          DATE,
                                  admin_remarks       TEXT,

                                  CONSTRAINT pk_school_documents      PRIMARY KEY (id),
                                  CONSTRAINT fk_document_school       FOREIGN KEY (school_id)
                                      REFERENCES schools (id)
                                      ON DELETE CASCADE,
                                  CONSTRAINT chk_document_type        CHECK (document_type IN (
                                                                                               'AFFILIATION_CERTIFICATE','UDISE_LETTER','REGISTRATION_CERTIFICATE',
                                                                                               'GOVERNMENT_NOC','TRUST_DEED','PAN_CARD','GST_CERTIFICATE',
                                                                                               'LOGO','SCHOOL_BUILDING_PHOTO','RECOGNITION_ORDER','OTHER'
                                      )),
                                  CONSTRAINT chk_file_size            CHECK (file_size_bytes IS NULL OR file_size_bytes > 0)
);

CREATE INDEX idx_doc_school_id  ON school_documents (school_id);
CREATE INDEX idx_doc_type       ON school_documents (document_type);
CREATE INDEX idx_doc_verified   ON school_documents (verified);
CREATE INDEX idx_doc_expires    ON school_documents (expires_on);

COMMENT ON TABLE  school_documents          IS 'Files uploaded for a school — affiliation certs, logo, NOC, etc.';
COMMENT ON COLUMN school_documents.verified IS 'Set to TRUE by a platform admin after manual review';


-- -----------------------------------------------------------------------------
-- 8. school_grade_ranges
--    Grade/class segments offered by a school (Pre-Primary, Primary, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE school_grade_ranges (
    -- BaseEntity columns
                                     id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                     created_at          TIMESTAMP       NOT NULL,
                                     updated_at          TIMESTAMP,
                                     created_by          UUID,
                                     updated_by          UUID,
                                     is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                                     deleted_at          TIMESTAMP,

    -- FK
                                     school_id           UUID            NOT NULL,

    -- Grade range
                                     segment_name        VARCHAR(100)    NOT NULL,
                                     from_grade          VARCHAR(20)     NOT NULL,
                                     to_grade            VARCHAR(20)     NOT NULL,
                                     from_grade_order    INTEGER         NOT NULL,
                                     to_grade_order      INTEGER         NOT NULL,
                                     board_override      VARCHAR(30),

                                     CONSTRAINT pk_school_grade_ranges   PRIMARY KEY (id),
                                     CONSTRAINT fk_grade_range_school    FOREIGN KEY (school_id)
                                         REFERENCES schools (id)
                                         ON DELETE CASCADE,
                                     CONSTRAINT chk_grade_order          CHECK (from_grade_order <= to_grade_order)
);

CREATE INDEX idx_grade_range_school_id  ON school_grade_ranges (school_id);

COMMENT ON TABLE  school_grade_ranges               IS 'Grade / class segments offered: Pre-Primary (Nursery–KG), Primary (1–5), etc.';
COMMENT ON COLUMN school_grade_ranges.from_grade    IS 'Textual label e.g. Nursery, 1, 6 — use from_grade_order for sorting';
COMMENT ON COLUMN school_grade_ranges.board_override IS 'Overrides school-level board for this segment (dual-board schools)';


-- -----------------------------------------------------------------------------
-- 9. school_facilities
--    Infrastructure checklist (Library, Pool, CCTV, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE school_facilities (
    -- BaseEntity columns
                                   id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                   created_at          TIMESTAMP       NOT NULL,
                                   updated_at          TIMESTAMP,
                                   created_by          UUID,
                                   updated_by          UUID,
                                   is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                                   deleted_at          TIMESTAMP,

    -- FK
                                   school_id           UUID            NOT NULL,

    -- Facility
                                   facility_type       VARCHAR(50)     NOT NULL,
                                   description         VARCHAR(500),
                                   is_available        BOOLEAN         NOT NULL DEFAULT TRUE,

                                   CONSTRAINT pk_school_facilities     PRIMARY KEY (id),
                                   CONSTRAINT fk_facility_school       FOREIGN KEY (school_id)
                                       REFERENCES schools (id)
                                       ON DELETE CASCADE,
                                   CONSTRAINT chk_facility_type        CHECK (facility_type IN (
                                                                                                'LIBRARY','COMPUTER_LAB','SCIENCE_LAB','SMART_CLASSROOM','AUDITORIUM',
                                                                                                'SPORTS_GROUND','SWIMMING_POOL','GYMNASIUM','TRANSPORT','HOSTEL',
                                                                                                'CANTEEN','MEDICAL_ROOM','CCTV','WIFI','SOLAR_POWER',
                                                                                                'RAMP_ACCESSIBILITY','OTHER'
                                       ))
);

CREATE INDEX idx_facility_school_id ON school_facilities (school_id);
CREATE INDEX idx_facility_type      ON school_facilities (facility_type);

COMMENT ON TABLE school_facilities IS 'Infrastructure items available at the school campus';


-- -----------------------------------------------------------------------------
-- 10. school_social_links
--     Social media / online presence URLs (Facebook, YouTube, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE school_social_links (
    -- BaseEntity columns
                                     id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                     created_at          TIMESTAMP       NOT NULL,
                                     updated_at          TIMESTAMP,
                                     created_by          UUID,
                                     updated_by          UUID,
                                     is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                                     deleted_at          TIMESTAMP,

    -- FK
                                     school_id           UUID            NOT NULL,

    -- Link
                                     platform            VARCHAR(30)     NOT NULL,
                                     url                 VARCHAR(512)    NOT NULL,

                                     CONSTRAINT pk_school_social_links   PRIMARY KEY (id),
                                     CONSTRAINT fk_social_link_school    FOREIGN KEY (school_id)
                                         REFERENCES schools (id)
                                         ON DELETE CASCADE,
                                     CONSTRAINT chk_social_platform      CHECK (platform IN (
                                                                                             'FACEBOOK','INSTAGRAM','TWITTER_X','YOUTUBE',
                                                                                             'LINKEDIN','WHATSAPP_CHANNEL','TELEGRAM','OTHER'
                                         )),
    -- One link per platform per school
                                     CONSTRAINT uq_school_platform       UNIQUE (school_id, platform)
);

CREATE INDEX idx_social_school_id   ON school_social_links (school_id);

COMMENT ON TABLE school_social_links IS 'Social media links for a school — one row per platform';


-- -----------------------------------------------------------------------------
-- 11. school_onboarding_audit
--     Immutable log of every onboarding step transition.
--     Never updated or soft-deleted — append-only.
-- -----------------------------------------------------------------------------
CREATE TABLE school_onboarding_audit (
    -- BaseEntity columns (updated_at / updated_by / deleted_* unused but kept for base compat)
                                         id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                         created_at          TIMESTAMP       NOT NULL,
                                         updated_at          TIMESTAMP,
                                         created_by          UUID,
                                         updated_by          UUID,
                                         is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                                         deleted_at          TIMESTAMP,

    -- FK
                                         school_id           UUID            NOT NULL,

    -- Audit data
                                         step                VARCHAR(30)     NOT NULL,
                                         action              VARCHAR(20)     NOT NULL,
                                         performed_by        UUID            NOT NULL,
                                         remarks             VARCHAR(500),

                                         CONSTRAINT pk_school_onboarding_audit   PRIMARY KEY (id),
                                         CONSTRAINT fk_audit_school              FOREIGN KEY (school_id)
                                             REFERENCES schools (id)
                                             ON DELETE CASCADE,
                                         CONSTRAINT chk_audit_step              CHECK (step IN (
                                                                                                'BASIC_INFO','CONTACT','ADDRESS','ACADEMIC','DOCUMENTS','COMPLETE'
                                             )),
                                         CONSTRAINT chk_audit_action            CHECK (action IN (
                                                                                                  'COMPLETED','SKIPPED','REVERTED'
                                             ))
);

CREATE INDEX idx_onboard_audit_school_id    ON school_onboarding_audit (school_id);
CREATE INDEX idx_onboard_audit_step         ON school_onboarding_audit (step);
CREATE INDEX idx_onboard_audit_created_at   ON school_onboarding_audit (created_at);

COMMENT ON TABLE  school_onboarding_audit               IS 'Immutable audit log — append-only, never soft-deleted';
COMMENT ON COLUMN school_onboarding_audit.action        IS 'COMPLETED | SKIPPED | REVERTED';
COMMENT ON COLUMN school_onboarding_audit.performed_by  IS 'UUID of the admin user who triggered the transition';


-- -----------------------------------------------------------------------------
-- 12. academic_years
--     One row per academic year per school (e.g. "2024-25").
--     Drives terms, shifts, timetables, fees, and reports.
-- -----------------------------------------------------------------------------
CREATE TABLE academic_years (
    -- BaseEntity columns
                                id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                                created_at          TIMESTAMP       NOT NULL,
                                updated_at          TIMESTAMP,
                                created_by          UUID,
                                updated_by          UUID,
                                is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                                deleted_at          TIMESTAMP,

    -- FK
                                school_id           UUID            NOT NULL,

    -- Year details
                                label               VARCHAR(20)     NOT NULL,
                                start_date          DATE            NOT NULL,
                                end_date            DATE            NOT NULL,
                                is_current          BOOLEAN         NOT NULL DEFAULT FALSE,
                                is_locked           BOOLEAN         NOT NULL DEFAULT FALSE,

                                CONSTRAINT pk_academic_years        PRIMARY KEY (id),
                                CONSTRAINT uq_ay_school_label       UNIQUE (school_id, label),
                                CONSTRAINT fk_ay_school             FOREIGN KEY (school_id)
                                    REFERENCES schools (id)
                                    ON DELETE CASCADE,
                                CONSTRAINT chk_ay_date_range        CHECK (end_date > start_date)
);

CREATE INDEX idx_ay_school_id   ON academic_years (school_id);
CREATE INDEX idx_ay_current     ON academic_years (is_current);
CREATE INDEX idx_ay_start_date  ON academic_years (start_date);

-- Only one current academic year per school (partial unique index)
CREATE UNIQUE INDEX uq_ay_school_current
    ON academic_years (school_id)
    WHERE is_current = TRUE AND is_deleted = FALSE;

COMMENT ON TABLE  academic_years            IS 'Academic years per school — label e.g. 2024-25';
COMMENT ON COLUMN academic_years.is_current IS 'Only one row per school can be TRUE — enforced by uq_ay_school_current';
COMMENT ON COLUMN academic_years.is_locked  IS 'Locked years are read-only — marks, fees, reports cannot be edited';


-- -----------------------------------------------------------------------------
-- 13. school_terms
--     Terms / semesters within an academic year (Term 1, Term 2, Q1…Q4).
-- -----------------------------------------------------------------------------
CREATE TABLE school_terms (
    -- BaseEntity columns
                              id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                              created_at          TIMESTAMP       NOT NULL,
                              updated_at          TIMESTAMP,
                              created_by          UUID,
                              updated_by          UUID,
                              is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                              deleted_at          TIMESTAMP,

    -- FK
                              academic_year_id    UUID            NOT NULL,

    -- Term details
                              name                VARCHAR(50)     NOT NULL,
                              sort_order          INTEGER         NOT NULL,
                              start_date          DATE            NOT NULL,
                              end_date            DATE            NOT NULL,
                              is_locked           BOOLEAN         NOT NULL DEFAULT FALSE,

                              CONSTRAINT pk_school_terms          PRIMARY KEY (id),
                              CONSTRAINT fk_term_academic_year    FOREIGN KEY (academic_year_id)
                                  REFERENCES academic_years (id)
                                  ON DELETE CASCADE,
                              CONSTRAINT chk_term_date_range      CHECK (end_date > start_date),
                              CONSTRAINT chk_term_sort_order      CHECK (sort_order > 0)
);

CREATE INDEX idx_term_academic_year_id  ON school_terms (academic_year_id);
CREATE INDEX idx_term_sort_order        ON school_terms (academic_year_id, sort_order);

COMMENT ON TABLE  school_terms              IS 'Terms / semesters within an academic year';
COMMENT ON COLUMN school_terms.sort_order   IS 'Display order: 1 = first term, 2 = second, etc.';


-- -----------------------------------------------------------------------------
-- 14. school_shifts
--     Shift timings within an academic year (Morning, Afternoon, Evening).
-- -----------------------------------------------------------------------------
CREATE TABLE school_shifts (
    -- BaseEntity columns
                               id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
                               created_at          TIMESTAMP       NOT NULL,
                               updated_at          TIMESTAMP,
                               created_by          UUID,
                               updated_by          UUID,
                               is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
                               deleted_at          TIMESTAMP,

    -- FK
                               academic_year_id    UUID            NOT NULL,

    -- Shift details
                               name                VARCHAR(100)    NOT NULL,
                               start_time          TIME            NOT NULL,
                               end_time            TIME            NOT NULL,
                               is_default          BOOLEAN         NOT NULL DEFAULT FALSE,

                               CONSTRAINT pk_school_shifts             PRIMARY KEY (id),
                               CONSTRAINT fk_shift_academic_year       FOREIGN KEY (academic_year_id)
                                   REFERENCES academic_years (id)
                                   ON DELETE CASCADE,
                               CONSTRAINT chk_shift_time_range         CHECK (end_time > start_time)
);

CREATE INDEX idx_shift_academic_year_id ON school_shifts (academic_year_id);

-- Only one default shift per academic year (partial unique index)
CREATE UNIQUE INDEX uq_ay_default_shift
    ON school_shifts (academic_year_id)
    WHERE is_default = TRUE AND is_deleted = FALSE;

COMMENT ON TABLE  school_shifts             IS 'Shift timings per academic year — Morning, Afternoon, Evening';
COMMENT ON COLUMN school_shifts.is_default  IS 'Only one shift per year can be TRUE — enforced by uq_ay_default_shift';


-- =============================================================================
-- END OF MIGRATION
-- =============================================================================