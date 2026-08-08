package com.bdreview.platform.common;

/**
 * Normalizes Bangladeshi phone numbers to E.164 (spec §5: "normalized
 * everywhere before storage or comparison"). Minimal/pragmatic — swap for a
 * proper libphonenumber-based normalizer if wider validation is needed.
 */
public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw new BadRequestException("Phone number is required");
        }
        String digits = raw.replaceAll("[^0-9+]", "");

        if (digits.startsWith("+880")) {
            return digits;
        }
        if (digits.startsWith("880")) {
            return "+" + digits;
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            return "+880" + digits.substring(1);
        }
        throw new BadRequestException("Invalid Bangladeshi phone number: " + raw);
    }
}
