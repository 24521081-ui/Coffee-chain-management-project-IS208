package com.phungloccoffee.util;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoCodeGenerator {
    private static final int DEFAULT_WIDTH = 3;

    private AutoCodeGenerator() {
    }

    public static String generateNextCode(String prefix, Collection<String> existingCodes) {
        return generateNextCode(prefix, existingCodes, DEFAULT_WIDTH);
    }

    public static String generateNextCode(String prefix, Collection<String> existingCodes, int width) {
        String normalizedPrefix = safe(prefix).toUpperCase(Locale.ROOT);
        Pattern pattern = Pattern.compile("^" + Pattern.quote(normalizedPrefix) + "(\\d+)$", Pattern.CASE_INSENSITIVE);
        int maxNumber = 0;
        if (existingCodes != null) {
            for (String code : existingCodes) {
                Matcher matcher = pattern.matcher(safe(code));
                if (matcher.matches()) {
                    try {
                        maxNumber = Math.max(maxNumber, Integer.parseInt(matcher.group(1)));
                    } catch (NumberFormatException ignored) {
                        // Ignore malformed numeric parts and continue with valid codes.
                    }
                }
            }
        }
        return normalizedPrefix + String.format(Locale.ROOT, "%0" + Math.max(width, DEFAULT_WIDTH) + "d", maxNumber + 1);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
