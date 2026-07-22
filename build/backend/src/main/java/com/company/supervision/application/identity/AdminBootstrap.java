package com.company.supervision.application.identity;
import com.company.supervision.domain.model.identity.Account;
import com.company.supervision.domain.model.identity.Role;
import com.company.supervision.infrastructure.repository.identity.AccountMapper;
import com.company.supervision.infrastructure.repository.identity.AccountRoleMapper;
import com.company.supervision.infrastructure.repository.identity.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component @Slf4j
public class AdminBootstrap implements CommandLineRunner {
 private final AccountMapper accounts; private final RoleMapper roles; private final AccountRoleMapper links; private final PasswordEncoder encoder;
 @Value("${supervision.bootstrap-admin.username:admin}") private String username;
 @Value("${supervision.bootstrap-admin.password:}") private String password;
 public AdminBootstrap(AccountMapper a,RoleMapper r,AccountRoleMapper l,PasswordEncoder e){accounts=a;roles=r;links=l;encoder=e;}
 public void run(String... args){ if(accounts.selectByUsername(username)!=null)return; if(password==null||password.isBlank()){log.warn("No initial administrator exists. Set SUPERVISION_ADMIN_PASSWORD for first startup.");return;} AuthService.validatePassword(password,username); Account a=new Account();a.setUsername(username);a.setDisplayName("System Administrator");a.setPasswordHash(encoder.encode(password));a.setStatus(1);a.setMustChangePassword(1);a.setFailedLoginCount(0);accounts.insert(a);Role role=roles.selectByCode("ADMIN");links.insert(a.getId(),role.getId());log.info("Initial administrator created; password change is required."); }
}