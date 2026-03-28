package com.ai.vidya.modules.school.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** Step 3 — Physical address and location */
@Data
public class AddressRequest {

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 255)
    private String landmark;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String district;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @DecimalMin(value = "-90.0",  message = "Latitude must be ≥ -90")
    @DecimalMax(value = "90.0",   message = "Latitude must be ≤ 90")
    @Digits(integer = 3, fraction = 7)
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be ≥ -180")
    @DecimalMax(value = "180.0",  message = "Longitude must be ≤ 180")
    @Digits(integer = 4, fraction = 7)
    private BigDecimal longitude;

    @Size(max = 512)
    private String mapLink;

    @Size(max = 100)
    private String googlePlaceId;

    private String directions;
}