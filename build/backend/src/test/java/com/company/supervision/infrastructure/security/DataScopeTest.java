package com.company.supervision.infrastructure.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DataScopeTest {
    @Test void administratorCanReadButDoesNotOwnAnotherAccount(){DataScope scope=new DataScope(1L,true);assertThat(scope.canRead(2L)).isTrue();assertThat(scope.owns(2L)).isFalse();}
    @Test void normalAccountCannotReadUnassignedOrAnotherOwner(){DataScope scope=new DataScope(1L,false);assertThat(scope.canRead(null)).isFalse();assertThat(scope.canRead(2L)).isFalse();assertThat(scope.canRead(1L)).isTrue();}
}
