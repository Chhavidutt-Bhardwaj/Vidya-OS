package com.ai.vidya.modules.fee.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface FeeDefaulterResponse {
    UUID       getStudentId();
    String     getStudentName();
    String     getAdmissionNo();
    BigDecimal getOverdueAmount();
    LocalDate  getOldestDueDate();
    Long       getOverdueCount();
}
