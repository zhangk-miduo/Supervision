package com.company.supervision.application.identity;

import com.company.supervision.domain.model.identity.Account;
import com.company.supervision.entity.dto.AccountScopedView;
import com.company.supervision.infrastructure.repository.identity.AccountMapper;
import com.company.supervision.infrastructure.security.DataScope;
import org.springframework.stereotype.Service;

@Service
public class CreatorViewService {
    private final AccountMapper accounts;

    public CreatorViewService(AccountMapper accounts) {
        this.accounts = accounts;
    }

    public void fill(AccountScopedView view, Long ownerAccountId, DataScope scope) {
        view.setCreatorAccountId(ownerAccountId);
        view.setOwnedByCurrentUser(scope.owns(ownerAccountId));
        view.setCanEdit(scope.owns(ownerAccountId));
        Account account = ownerAccountId == null ? null : accounts.selectById(ownerAccountId);
        if (account != null) {
            view.setCreatorUsername(account.getUsername());
            view.setCreatorDisplayName(account.getDisplayName());
        } else if (ownerAccountId == null) {
            view.setCreatorUsername("unassigned");
            view.setCreatorDisplayName("历史数据未归属");
        }
    }

    public Account find(Long ownerAccountId) {
        return ownerAccountId == null ? null : accounts.selectById(ownerAccountId);
    }
}
