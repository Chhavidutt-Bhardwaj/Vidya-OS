package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Configuration / preference settings for a school.
 * One-to-one with School — loaded only when settings are needed.
 */
@Entity
@Table(name = "school_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    // ── Locale & Regional ──────────────────────────────────────────────────

    /** Primary locale e.g. "en-IN", "hi-IN" */
    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "en-IN";

    /** Timezone e.g. "Asia/Kolkata" */
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    /** Currency code e.g. "INR" */
    @Column(name = "currency", length = 5)
    @Builder.Default
    private String currency = "INR";

    // ── Date / Academic ────────────────────────────────────────────────────

    /** Date format shown across the app: "DD/MM/YYYY" or "MM/DD/YYYY" */
    @Column(name = "date_format", length = 20)
    @Builder.Default
    private String dateFormat = "DD/MM/YYYY";

    /** Academic year start month (1–12) */
    @Column(name = "academic_year_start_month", nullable = false)
    @Builder.Default
    private int academicYearStartMonth = 4; // April

    // ── Attendance ─────────────────────────────────────────────────────────

    /** Minimum attendance % required for promotion */
    @Column(name = "min_attendance_pct")
    @Builder.Default
    private int minAttendancePct = 75;

    /** Whether Saturday is a working day */
    @Column(name = "saturday_working", nullable = false)
    @Builder.Default
    private boolean saturdayWorking = true;

    /** Whether alternate Saturdays are off */
    @Column(name = "alternate_saturday_off", nullable = false)
    @Builder.Default
    private boolean alternateSaturdayOff = false;

    // ── Fee ────────────────────────────────────────────────────────────────

    /** Whether GST is applicable on fees */
    @Column(name = "gst_applicable", nullable = false)
    @Builder.Default
    private boolean gstApplicable = false;

    @Column(name = "gstin", length = 20)
    private String gstin;

    /** Grace period in days before late fee kicks in */
    @Column(name = "fee_due_grace_days", nullable = false)
    @Builder.Default
    private int feeDueGraceDays = 5;

    // ── Notifications ──────────────────────────────────────────────────────

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private boolean smsEnabled = false;

    @Column(name = "whatsapp_enabled", nullable = false)
    @Builder.Default
    private boolean whatsappEnabled = false;

    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean emailNotificationsEnabled = true;

    // ── Branding ───────────────────────────────────────────────────────────

    /** Hex color for school's primary brand color on reports */
    @Column(name = "brand_color_primary", length = 10)
    private String brandColorPrimary;

    @Column(name = "brand_color_secondary", length = 10)
    private String brandColorSecondary;
}