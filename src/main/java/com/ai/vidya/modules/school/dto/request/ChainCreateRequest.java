package com.ai.vidya.modules.school.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request for POST /api/v1/chains
 *
 * Creates a new school chain entity and provisions a CHAIN_ADMIN user
 * who can subsequently onboard branches under this chain.
 */
@Data
public class ChainCreateRequest {

    // ── Chain identity ─────────────────────────────────────────────────────

    @NotBlank(message = "Chain name is required")
    @Size(max = 255)
    private String name;

    /**
     * Short uppercase code — unique across chains.
     * e.g. "DPS", "RYAN", "KV"
     */
    @NotBlank(message = "Chain code is required")
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z0-9_-]{2,30}$",
             message = "Chain code must be 2–30 uppercase alphanumeric characters")
    private String chainCode;

    @Size(max = 1000)
    private String description;

    @Size(max = 255)
    private String website;

    // ── Chain Admin account ────────────────────────────────────────────────

    /**
     * Email for the CHAIN_ADMIN user.
     * If null, auto-generated as: admin.<chainCode-lower>@vidya.ai
     */
    @Email(message = "Admin email must be valid")
    @Size(max = 255)
    private String adminEmail;

    @NotBlank(message = "Admin full name is required")
    @Size(max = 150)
    private String adminFullName;

    @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
    private String adminPhone;
}
