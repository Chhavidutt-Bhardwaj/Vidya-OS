-- =============================================================================
-- V10__library_hostel_transport.sql
-- Library, Hostel, Transport module schemas
-- =============================================================================

-- =============================================================================
-- LIBRARY
-- =============================================================================
CREATE TABLE library_books (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,

    school_id           UUID        NOT NULL REFERENCES schools(id),
    isbn                VARCHAR(20),
    title               VARCHAR(300) NOT NULL,
    author              VARCHAR(200) NOT NULL,
    publisher           VARCHAR(200),
    edition             VARCHAR(50),
    publication_year    INTEGER,
    category            VARCHAR(100),
    language            VARCHAR(30) NOT NULL DEFAULT 'English',
    total_copies        INTEGER     NOT NULL DEFAULT 1,
    available_copies    INTEGER     NOT NULL DEFAULT 1,
    rack_no             VARCHAR(30),
    price               NUMERIC(10,2),
    cover_image_url     VARCHAR(512),

    CONSTRAINT pk_library_books  PRIMARY KEY (id)
);

CREATE INDEX idx_lb_school_id ON library_books(school_id);
CREATE INDEX idx_lb_isbn      ON library_books(isbn);
CREATE INDEX idx_lb_title     ON library_books(school_id, title);

CREATE TABLE library_issues (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,

    school_id       UUID        NOT NULL,
    book_id         UUID        NOT NULL REFERENCES library_books(id),
    borrower_id     UUID        NOT NULL,     -- student_id or staff_id
    borrower_type   VARCHAR(10) NOT NULL CHECK (borrower_type IN ('STUDENT','STAFF')),
    issue_date      DATE        NOT NULL,
    due_date        DATE        NOT NULL,
    return_date     DATE,
    fine_amount     NUMERIC(8,2) NOT NULL DEFAULT 0,
    fine_paid       BOOLEAN     NOT NULL DEFAULT FALSE,
    status          VARCHAR(15) NOT NULL DEFAULT 'ISSUED'
                    CHECK (status IN ('ISSUED','RETURNED','OVERDUE','LOST')),
    issued_by       UUID        NOT NULL,
    returned_to     UUID,
    remarks         VARCHAR(300),

    CONSTRAINT pk_library_issues PRIMARY KEY (id)
);

CREATE INDEX idx_li_book_id     ON library_issues(book_id);
CREATE INDEX idx_li_borrower    ON library_issues(borrower_id, borrower_type);
CREATE INDEX idx_li_status      ON library_issues(status, due_date);

-- =============================================================================
-- HOSTEL
-- =============================================================================
CREATE TABLE hostel_blocks (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    school_id   UUID        NOT NULL REFERENCES schools(id),
    block_name  VARCHAR(100) NOT NULL,
    gender      VARCHAR(10) NOT NULL CHECK (gender IN ('MALE','FEMALE','MIXED')),
    warden_id   UUID,
    total_rooms INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT pk_hostel_blocks PRIMARY KEY (id)
);

CREATE TABLE hostel_rooms (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    block_id        UUID        NOT NULL REFERENCES hostel_blocks(id),
    room_number     VARCHAR(20) NOT NULL,
    room_type       VARCHAR(20) NOT NULL CHECK (room_type IN ('SINGLE','DOUBLE','TRIPLE','DORMITORY')),
    capacity        INTEGER     NOT NULL DEFAULT 1,
    occupancy       INTEGER     NOT NULL DEFAULT 0,
    floor           INTEGER,
    monthly_fee     NUMERIC(10,2),
    CONSTRAINT pk_hostel_rooms PRIMARY KEY (id),
    CONSTRAINT uq_room_no UNIQUE (block_id, room_number)
);

CREATE TABLE hostel_allocations (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    room_id         UUID        NOT NULL REFERENCES hostel_rooms(id),
    student_id      UUID        NOT NULL REFERENCES students(id),
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    allotted_on     DATE        NOT NULL,
    vacated_on      DATE,
    status          VARCHAR(15) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE','VACATED','TRANSFERRED')),
    CONSTRAINT pk_hostel_alloc    PRIMARY KEY (id),
    CONSTRAINT uq_hostel_student  UNIQUE (student_id, academic_year_id)
);

CREATE INDEX idx_ha_room_id    ON hostel_allocations(room_id);
CREATE INDEX idx_ha_student_id ON hostel_allocations(student_id);

-- =============================================================================
-- TRANSPORT
-- =============================================================================
CREATE TABLE transport_routes (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    school_id       UUID        NOT NULL REFERENCES schools(id),
    route_name      VARCHAR(100) NOT NULL,
    route_code      VARCHAR(20) NOT NULL,
    start_point     VARCHAR(200) NOT NULL,
    end_point       VARCHAR(200) NOT NULL,
    distance_km     NUMERIC(8,2),
    monthly_fee     NUMERIC(10,2),
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_transport_routes PRIMARY KEY (id),
    CONSTRAINT uq_route_code UNIQUE (school_id, route_code)
);

CREATE TABLE transport_vehicles (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    school_id       UUID        NOT NULL REFERENCES schools(id),
    route_id        UUID        REFERENCES transport_routes(id),
    vehicle_no      VARCHAR(20) NOT NULL,
    vehicle_type    VARCHAR(30) NOT NULL,
    capacity        INTEGER     NOT NULL,
    driver_name     VARCHAR(150),
    driver_mobile   VARCHAR(15),
    driver_license  VARCHAR(30),
    tracker_id      VARCHAR(50),
    insurance_expiry DATE,
    fitness_expiry  DATE,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_transport_vehicles PRIMARY KEY (id),
    CONSTRAINT uq_vehicle_no UNIQUE (school_id, vehicle_no)
);

CREATE TABLE transport_allocations (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    student_id      UUID        NOT NULL REFERENCES students(id),
    vehicle_id      UUID        NOT NULL REFERENCES transport_vehicles(id),
    route_id        UUID        NOT NULL REFERENCES transport_routes(id),
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    pickup_point    VARCHAR(200),
    pickup_time     TIME,
    drop_time       TIME,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_transport_alloc   PRIMARY KEY (id),
    CONSTRAINT uq_transport_student UNIQUE (student_id, academic_year_id)
);

-- =============================================================================
-- END V10
-- =============================================================================
