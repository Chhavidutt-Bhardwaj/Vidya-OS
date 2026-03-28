package com.ai.vidya.modules.school.service;

import com.ai.vidya.modules.user.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Generates admin email addresses and secure temporary passwords
 * for new school admin accounts created during onboarding.
 *
 * Email format:   admin.<slug>@vidya.ai
 *   e.g. "Springfield High School" → admin.springfield-high@vidya.ai
 *
 * If that email is taken (duplicate school name), a numeric suffix is appended:
 *   admin.springfield-high-2@vidya.ai
 *
 * Password format: 12 characters — uppercase + lowercase + digits + special chars.
 * The raw password is returned once to the caller and shown to the platform admin.
 * Only the BCrypt hash is stored in the DB.
 */
@Service
@RequiredArgsConstructor
public class UsernamePasswordGeneratorService {

    private final SystemUserRepository userRepository;

    private static final String DOMAIN       = "vidya.ai";
    private static final String PREFIX       = "admin.";
    private static final int    MAX_SLUG_LEN = 30;
    private static final int    PASSWORD_LEN = 12;

    private static final String UPPER   = "ABCDEFGHJKLMNPQRSTUVWXYZ";   // no I, O
    private static final String LOWER   = "abcdefghjkmnpqrstuvwxyz";    // no i, l, o
    private static final String DIGITS  = "23456789";                    // no 0, 1
    private static final String SPECIAL = "@#$!%?&";
    private static final String ALL     = UPPER + LOWER + DIGITS + SPECIAL;

    private static final SecureRandom RNG = new SecureRandom();

    // ── Email generation ─────────────────────────────────────────────────

    /**
     * Generates a unique admin email for the school.
     * Checks DB for collision and appends a numeric suffix if needed.
     */
    public String generateAdminEmail(String schoolName) {
        String slug   = slugify(schoolName);
        String base   = PREFIX + slug + "@" + DOMAIN;

        if (!userRepository.existsByEmailAndDeletedFalse(base)) {
            return base;
        }

        // Collision → try with numeric suffix 2..99
        for (int i = 2; i <= 99; i++) {
            String candidate = PREFIX + slug + "-" + i + "@" + DOMAIN;
            if (!userRepository.existsByEmailAndDeletedFalse(candidate)) {
                return candidate;
            }
        }
        // Extremely unlikely — fall back to UUID suffix
        return PREFIX + slug + "-" + System.currentTimeMillis() % 10000 + "@" + DOMAIN;
    }

    /**
     * Converts a school name to a URL-safe lowercase slug.
     *   "Delhi Public School" → "delhi-public-school"
     *   "St. Xavier's Academy" → "st-xaviers-academy"
     */
    public String slugify(String name) {
        if (name == null || name.isBlank()) return "school";

        // Normalize unicode, strip diacritics
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        normalized = Pattern.compile("[^\\p{ASCII}]").matcher(normalized).replaceAll("");

        String slug = normalized
            .toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")   // keep alphanumeric, spaces, hyphens
            .trim()
            .replaceAll("\\s+", "-")            // spaces → hyphens
            .replaceAll("-+", "-")              // collapse multiple hyphens
            .replaceAll("^-|-$", "");           // trim leading/trailing hyphens

        // Trim common noise words from the end to keep slug short
        for (String noise : new String[]{"school", "academy", "vidyalaya", "institution"}) {
            if (slug.endsWith("-" + noise) && slug.length() > noise.length() + 1) {
                slug = slug.substring(0, slug.length() - noise.length() - 1);
                break;
            }
        }

        // Hard cap
        if (slug.length() > MAX_SLUG_LEN) {
            slug = slug.substring(0, MAX_SLUG_LEN).replaceAll("-$", "");
        }

        return slug.isEmpty() ? "school" : slug;
    }

    // ── Password generation ───────────────────────────────────────────────

    /**
     * Generates a cryptographically secure temporary password.
     * Guarantees at least one character from each character class.
     *
     * Example output: "Zp7#Kq3mVr!9"
     */
    public String generateTemporaryPassword() {
        char[] password = new char[PASSWORD_LEN];

        // Guarantee at least 1 of each required class
        password[0] = UPPER.charAt(RNG.nextInt(UPPER.length()));
        password[1] = LOWER.charAt(RNG.nextInt(LOWER.length()));
        password[2] = DIGITS.charAt(RNG.nextInt(DIGITS.length()));
        password[3] = SPECIAL.charAt(RNG.nextInt(SPECIAL.length()));

        // Fill remaining positions from the full pool
        for (int i = 4; i < PASSWORD_LEN; i++) {
            password[i] = ALL.charAt(RNG.nextInt(ALL.length()));
        }

        // Shuffle to avoid predictable pattern (first 4 always being upper/lower/digit/special)
        for (int i = PASSWORD_LEN - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }

        return new String(password);
    }
}
