package com.company.supervision.infrastructure.security;

import java.util.regex.Pattern;

public final class SensitiveDataRedactor {
    private static final Pattern WEBHOOK_KEY = Pattern.compile("(?i)([?&]key=)[^&\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTHORIZATION = Pattern.compile("(?i)(Authorization\\s*[:=]\\s*(?:Bearer\\s+)?)[^,;\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECRET = Pattern.compile("(?i)((?:secret|password|token)\\s*[:=]\\s*)[^,;\\s]+", Pattern.CASE_INSENSITIVE);

    private SensitiveDataRedactor() {}

    public static String redact(String value) {
        if (value == null) return null;
        String result = WEBHOOK_KEY.matcher(value).replaceAll("$1***");
        result = AUTHORIZATION.matcher(result).replaceAll("$1***");
        return SECRET.matcher(result).replaceAll("$1***");
    }
}