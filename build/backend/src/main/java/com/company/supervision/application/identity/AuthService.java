package com.company.supervision.application.identity;

import com.company.supervision.domain.model.identity.Account;
import com.company.supervision.domain.model.identity.LoginAudit;
import com.company.supervision.domain.model.identity.Role;
import com.company.supervision.entity.dto.auth.ChangePasswordRequest;
import com.company.supervision.entity.dto.auth.LoginRequest;
import com.company.supervision.entity.dto.auth.LoginResponse;
import com.company.supervision.infrastructure.repository.identity.AccountMapper;
import com.company.supervision.infrastructure.repository.identity.LoginAuditMapper;
import com.company.supervision.infrastructure.repository.identity.RoleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private static final String SESSION_PREFIX = "supervision:auth:session:";
    private static final String ACCOUNT_SESSIONS_PREFIX = "supervision:auth:account-sessions:";
    private static final int MAX_FAILURES = 5;
    private final AccountMapper accountMapper;
    private final RoleMapper roleMapper;
    private final LoginAuditMapper auditMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder encoder;

    public AuthService(AccountMapper accountMapper, RoleMapper roleMapper, LoginAuditMapper auditMapper,
                       StringRedisTemplate redis, ObjectMapper objectMapper, PasswordEncoder encoder) {
        this.accountMapper=accountMapper; this.roleMapper=roleMapper; this.auditMapper=auditMapper;
        this.redis=redis; this.objectMapper=objectMapper; this.encoder=encoder;
    }

    public LoginResponse login(LoginRequest request, String ip) {
        Account account=accountMapper.selectByUsername(request.getUsername());
        if(account==null || account.getStatus()==null || account.getStatus()!=1) {
            audit(null, request.getUsername(), "LOGIN", false, ip, "invalid credentials");
            throw new IllegalArgumentException("Invalid username or password");
        }
        if(account.getLockedUntil()!=null && account.getLockedUntil().isAfter(LocalDateTime.now())) {
            audit(account.getId(), account.getUsername(), "LOGIN_LOCKED", false, ip, "account locked");
            throw new IllegalStateException("Account is temporarily locked");
        }
        if(!encoder.matches(request.getPassword(), account.getPasswordHash())) {
            int failures=(account.getFailedLoginCount()==null?0:account.getFailedLoginCount())+1;
            account.setFailedLoginCount(failures);
            if(failures>=MAX_FAILURES) account.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            accountMapper.updateById(account);
            audit(account.getId(), account.getUsername(), "LOGIN", false, ip, "invalid credentials");
            throw new IllegalArgumentException("Invalid username or password");
        }
        account.setFailedLoginCount(0); account.setLockedUntil(null); account.setLastLoginAt(LocalDateTime.now());
        accountMapper.updateById(account);
        List<String> roles=roleMapper.selectByAccountId(account.getId()).stream().map(Role::getCode).toList();
        boolean limited=Integer.valueOf(1).equals(account.getMustChangePassword());
        String token=createSession(account, roles, limited);
        audit(account.getId(), account.getUsername(), "LOGIN", true, ip, limited?"password change required":"success");
        return new LoginResponse(token, account.getId(), account.getUsername(), account.getDisplayName(), limited, roles);
    }

    public void changePassword(String token, ChangePasswordRequest request, String ip) {
        SessionInfo session=requireSession(token, true);
        Account account=accountMapper.selectById(session.getAccountId());
        if(account==null || !encoder.matches(request.getCurrentPassword(), account.getPasswordHash())) throw new IllegalArgumentException("Current password is incorrect");
        validatePassword(request.getNewPassword(), account.getUsername());
        account.setPasswordHash(encoder.encode(request.getNewPassword())); account.setMustChangePassword(0);
        account.setPasswordChangedAt(LocalDateTime.now()); account.setFailedLoginCount(0); account.setLockedUntil(null);
        accountMapper.updateById(account); revoke(token);
        audit(account.getId(), account.getUsername(), "PASSWORD_CHANGED", true, ip, "sessions revoked");
    }

    public SessionInfo requireSession(String token, boolean allowLimited) {
        if(token==null || token.isBlank()) throw new SecurityException("Authentication required");
        String raw=redis.opsForValue().get(SESSION_PREFIX+token);
        if(raw==null) throw new SecurityException("Session expired");
        try {
            SessionInfo info=objectMapper.readValue(raw, SessionInfo.class);
            if(info.isPasswordChangeOnly() && !allowLimited) throw new SecurityException("Password change required");
            return info;
        } catch(SecurityException e){ throw e; } catch(Exception e){ throw new SecurityException("Invalid session"); }
    }

    public void revoke(String token){
        if(token==null)return;
        try { SessionInfo info=requireSession(token,true); redis.opsForSet().remove(ACCOUNT_SESSIONS_PREFIX+info.getAccountId(),token); } catch(Exception ignore) {}
        redis.delete(SESSION_PREFIX+token);
    }

    public void revokeAll(Long accountId){
        var tokens=redis.opsForSet().members(ACCOUNT_SESSIONS_PREFIX+accountId);
        if(tokens!=null) tokens.forEach(t->redis.delete(SESSION_PREFIX+t));
        redis.delete(ACCOUNT_SESSIONS_PREFIX+accountId);
    }

    private String createSession(Account account, List<String> roles, boolean limited) {
        try {
            String token=UUID.randomUUID().toString().replace("-","");
            SessionInfo info=new SessionInfo(); info.setAccountId(account.getId()); info.setUsername(account.getUsername());
            info.setDisplayName(account.getDisplayName()); info.setRoles(roles); info.setPasswordChangeOnly(limited);
            Duration ttl=limited?Duration.ofMinutes(15):Duration.ofHours(8);
            redis.opsForValue().set(SESSION_PREFIX+token, objectMapper.writeValueAsString(info), ttl);
            redis.opsForSet().add(ACCOUNT_SESSIONS_PREFIX+account.getId(),token);
            redis.expire(ACCOUNT_SESSIONS_PREFIX+account.getId(),Duration.ofHours(9));
            return token;
        } catch(Exception e){ throw new IllegalStateException("Unable to create session", e); }
    }

    public static void validatePassword(String password, String username) {
        if(password==null || password.length()<10 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"))
            throw new IllegalArgumentException("Password must be at least 10 characters and include letters and numbers");
        if(username!=null && password.toLowerCase().contains(username.toLowerCase())) throw new IllegalArgumentException("Password must not contain username");
    }

    private void audit(Long id,String username,String event,boolean success,String ip,String detail){ LoginAudit a=new LoginAudit(); a.setAccountId(id); a.setUsername(username); a.setEventType(event); a.setSuccess(success?1:0); a.setIpAddress(ip); a.setDetail(detail); auditMapper.insert(a); }

    @Data public static class SessionInfo { private Long accountId; private String username; private String displayName; private List<String> roles; private boolean passwordChangeOnly; public boolean isAdmin(){ return roles!=null && roles.contains("ADMIN"); } }
}