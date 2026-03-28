package com.ai.vidya.modules.student.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

/**
 * Thread-safe admission number generator using a DB sequence table.
 * Format: ADM-2025-000001
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionNumberGenerator {

    private final JdbcTemplate jdbcTemplate;
    private static final String PREFIX = "ADM";

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate(UUID schoolId) {
        String yearPrefix = String.valueOf(Year.now().getValue());

        // Upsert + increment atomically
        jdbcTemplate.update("""
            INSERT INTO admission_number_sequences(school_id, year_prefix, last_number)
            VALUES (?, ?, 1)
            ON CONFLICT (school_id, year_prefix)
            DO UPDATE SET last_number = admission_number_sequences.last_number + 1
            """, schoolId, yearPrefix);

        Long seq = jdbcTemplate.queryForObject(
            "SELECT last_number FROM admission_number_sequences WHERE school_id = ? AND year_prefix = ?",
            Long.class, schoolId, yearPrefix);

        String admissionNo = String.format("%s-%s-%06d", PREFIX, yearPrefix, seq);
        log.debug("Generated admission number: {}", admissionNo);
        return admissionNo;
    }
}
