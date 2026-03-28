package com.ai.vidya.modules.school.dto.request;

import com.ai.vidya.common.enums.ContactType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/** Step 2 — School contacts (principal, admin, accounts, etc.) */
@Data
public class ContactRequest {

    @NotNull
    @Size(min = 1, message = "At least one contact is required")
    @Valid
    private List<ContactEntry> contacts;

    @Data
    public static class ContactEntry {

        @NotNull(message = "Contact type is required")
        private ContactType contactType;

        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        private String fullName;

        @Size(max = 100)
        private String designation;

        @Email(message = "Contact email must be valid")
        @Size(max = 255)
        private String email;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
        private String phone;

        @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
        private String phoneAlternate;

        private boolean primary              = false;
        private boolean receiveNotifications = true;
    }
}