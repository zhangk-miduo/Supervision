package com.company.supervision.infrastructure.security;

import com.company.supervision.application.identity.AuthService;
import java.util.Objects;

public record DataScope(Long accountId, boolean admin) {
    public static DataScope from(AuthService.SessionInfo session) {
        if (session == null || session.getAccountId() == null) throw new SecurityException("Authentication required");
        return new DataScope(session.getAccountId(), session.isAdmin());
    }

    public boolean canRead(Long ownerAccountId) {
        return admin || owns(ownerAccountId);
    }

    public boolean owns(Long ownerAccountId) {
        return ownerAccountId != null && Objects.equals(accountId, ownerAccountId);
    }
}
