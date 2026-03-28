package com.ai.vidya.modules.staff.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FeedbackRequest {

    @NotBlank
    private String feedbackSource;   // STUDENT | PARENT | PEER | ADMIN

    @NotNull @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 2000)
    private String comments;

    @NotBlank
    private String academicYear;    // e.g. "2024-25"

    @NotNull @PastOrPresent
    private LocalDate feedbackDate;
}
