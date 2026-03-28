package com.ai.vidya.modules.fee.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate(UUID schoolId) {
        String yearPrefix = String.valueOf(Year.now().getValue());

        jdbcTemplate.update("""
            INSERT INTO receipt_number_sequences(school_id, year_prefix, last_number)
            VALUES (?, ?, 1)
            ON CONFLICT (school_id, year_prefix)
            DO UPDATE SET last_number = receipt_number_sequences.last_number + 1
            """, schoolId, yearPrefix);

        Long seq = jdbcTemplate.queryForObject(
            "SELECT last_number FROM receipt_number_sequences WHERE school_id = ? AND year_prefix = ?",
            Long.class, schoolId, yearPrefix);

        return String.format("RCP-%s-%07d", yearPrefix, seq);
    }
}
