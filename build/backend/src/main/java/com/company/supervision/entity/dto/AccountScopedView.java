package com.company.supervision.entity.dto;

import lombok.Data;

@Data
public class AccountScopedView {
    private Long creatorAccountId;
    private String creatorUsername;
    private String creatorDisplayName;
    private boolean ownedByCurrentUser;
    private boolean canEdit;
}
