package com.company.supervision.infrastructure.migration;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDataScopeMigrationTest {
    @Test void migrationContainsOwnershipBackfillAndPrivateDefault() throws Exception {
        try(var in=getClass().getResourceAsStream("/db/migration/V12__account_data_scope_public_robots.sql")){
            assertThat(in).isNotNull();String sql=new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("owner_account_id","created_by","is_public","DEFAULT 0","uk_wecom_group_owner_name");
            assertThat(sql).doesNotContain("DROP TABLE");
        }
    }
}
