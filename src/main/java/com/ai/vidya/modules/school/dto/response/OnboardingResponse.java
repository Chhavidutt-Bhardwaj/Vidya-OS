package com.ai.vidya.modules.school.dto.response;

import com.ai.vidya.common.enums.OnboardingStep;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Returned after every onboarding step — tells the client where it stands */
@Data
@Builder
public class OnboardingResponse {

    private UUID           schoolId;
    private String         schoolName;
    private OnboardingStep currentStep;
    private OnboardingStep nextStep;
    private boolean        onboardingComplete;

    /** Only populated on step 1 — contains the auto-created admin credentials */
    private AdminCredentials adminCredentials;

    private String message;

    @Data
    @Builder
    public static class AdminCredentials {
        private String email;
        private String temporaryPassword;   // shown once — must be changed on first login
        private String username;            // same as email in this system
        private String loginUrl;
        private String note;
    }
}