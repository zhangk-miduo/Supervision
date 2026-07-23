package com.company.supervision.application.identity;

import com.company.supervision.domain.model.identity.Account;
import com.company.supervision.domain.model.organization.Person;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AuthServiceTest {
    @Test void acceptsStrongPassword(){assertThatCode(()->AuthService.validatePassword("StrongPass123","admin")).doesNotThrowAnyException();}

    @Test void rejectsWeakAndUsernamePasswords(){assertThatThrownBy(()->AuthService.validatePassword("short","admin")).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->AuthService.validatePassword("adminPassword123","admin")).isInstanceOf(IllegalArgumentException.class);}

    @Test void resolvesDisplayNameFromBoundPersonFirst(){
        Account account=new Account();account.setUsername("zhangsan");account.setDisplayName("Account Name");
        Person person=new Person();person.setName("Zhang San");
        assertThat(AuthService.resolveDisplayName(account,person)).isEqualTo("Zhang San");
    }

    @Test void fallsBackToAccountDisplayNameAndUsername(){
        Account account=new Account();account.setUsername("zhangsan");account.setDisplayName("Account Name");
        assertThat(AuthService.resolveDisplayName(account,null)).isEqualTo("Account Name");
        account.setDisplayName(" ");
        assertThat(AuthService.resolveDisplayName(account,null)).isEqualTo("zhangsan");
    }
}
