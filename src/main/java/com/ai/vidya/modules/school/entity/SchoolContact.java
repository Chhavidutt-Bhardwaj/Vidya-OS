package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.ContactType;
import jakarta.persistence.*;
import lombok.*;

/**
 * A contact person for a school — principal, admin, accounts, etc.
 * A school can have multiple contacts; one is marked primary.
 *
 * Many-to-one with School.
 */
@Entity
@Table(
    name = "school_contacts",
    indexes = {
        @Index(name = "idx_contact_school_id", columnList = "school_id"),
        @Index(name = "idx_contact_type",       columnList = "contact_type"),
        @Index(name = "idx_contact_primary",    columnList = "is_primary")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /** PRINCIPAL, VICE_PRINCIPAL, ADMIN, ACCOUNTS, ADMISSION, SUPPORT */
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 30)
    private ContactType contactType;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "phone_alternate", length = 20)
    private String phoneAlternate;

    /** If true, this contact appears first in the directory */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    /** Whether this contact should receive system notifications */
    @Column(name = "receive_notifications", nullable = false)
    @Builder.Default
    private boolean receiveNotifications = true;
}