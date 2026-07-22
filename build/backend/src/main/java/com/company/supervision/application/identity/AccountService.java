package com.company.supervision.application.identity;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.supervision.domain.model.identity.Account;
import com.company.supervision.domain.model.identity.Role;
import com.company.supervision.entity.dto.auth.AccountRequest;
import com.company.supervision.infrastructure.repository.identity.AccountMapper;
import com.company.supervision.infrastructure.repository.identity.AccountRoleMapper;
import com.company.supervision.infrastructure.repository.identity.RoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {
    private final AccountMapper accounts; private final RoleMapper roles; private final AccountRoleMapper accountRoles; private final PasswordEncoder encoder; private final AuthService auth;
    public AccountService(AccountMapper accounts,RoleMapper roles,AccountRoleMapper accountRoles,PasswordEncoder encoder,AuthService auth){this.accounts=accounts;this.roles=roles;this.accountRoles=accountRoles;this.encoder=encoder;this.auth=auth;}
    public IPage<Account> list(int page,int size){ IPage<Account> result=accounts.selectPage(new Page<>(Math.max(1,page),Math.max(1,size)),null); result.getRecords().forEach(a->a.setPasswordHash(null)); return result; }
    @Transactional public Long create(AccountRequest req,Long creator){ if(accounts.selectByUsername(req.getUsername())!=null) throw new IllegalArgumentException("Username already exists"); AuthService.validatePassword(req.getTemporaryPassword(),req.getUsername()); Account a=new Account(); a.setUsername(req.getUsername()); a.setDisplayName(req.getDisplayName()); a.setPasswordHash(encoder.encode(req.getTemporaryPassword())); a.setPersonId(req.getPersonId()); a.setStatus(req.getStatus()==null?1:req.getStatus()); a.setMustChangePassword(1); a.setFailedLoginCount(0); a.setCreatedBy(creator); accounts.insert(a); replaceRoles(a.getId(),req.getRoles()); return a.getId(); }
    @Transactional public void update(Long id,AccountRequest req){ Account a=required(id); a.setDisplayName(req.getDisplayName()); a.setPersonId(req.getPersonId()); a.setStatus(req.getStatus()==null?a.getStatus():req.getStatus()); accounts.updateById(a); replaceRoles(id,req.getRoles()); }
    public void resetPassword(Long id,String temporaryPassword){ Account a=required(id); AuthService.validatePassword(temporaryPassword,a.getUsername()); a.setPasswordHash(encoder.encode(temporaryPassword)); a.setMustChangePassword(1); a.setPasswordChangedAt(LocalDateTime.now()); a.setFailedLoginCount(0); a.setLockedUntil(null); accounts.updateById(a); auth.revokeAll(id); }
    private Account required(Long id){Account a=accounts.selectById(id);if(a==null)throw new IllegalArgumentException("Account not found");return a;}
    private void replaceRoles(Long id,List<String> codes){accountRoles.deleteByAccountId(id); if(codes==null||codes.isEmpty())codes=List.of("VIEWER"); for(String code:codes){Role r=roles.selectByCode(code);if(r==null)throw new IllegalArgumentException("Unknown role: "+code);accountRoles.insert(id,r.getId());}}
}