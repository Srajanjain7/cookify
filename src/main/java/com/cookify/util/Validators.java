package com.cookify.util;

import java.util.regex.Pattern;

/**
 * Named after the assignment's pseudocode functions (isUserIDUnique,
 * validatePassword, validateEmailFormat, ...) so the implementation
 * traces back to the Criterion B pseudocode directly.
 *
 * "Restricted symbols" is never defined in the source material (flagged
 * as an ambiguity in the requirements analysis). Interpreted here as:
 * no whitespace/control characters, everything else allowed -- matches
 * modern password guidance (NIST 800-63B) and avoids blacklisting
 * ordinary special characters the assignment never names.
 */
public final class Validators {

    private static final int MIN_PASSWORD_LENGTH = 9;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validators() {
    }

    public static boolean validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        return password.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c));
    }

    public static boolean validateEmailFormat(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean looksLikeEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }
}
