package com.jet.align.integration;

import com.jet.align.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsApplyAndEntitiesValidateAgainstTheSchema() {
        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);

        assertThat(applied).isGreaterThanOrEqualTo(15);
    }

    @Test
    void habitCompletionsForeignKeyCascadesOnDelete() {
        String deleteRule = jdbc.queryForObject("""
                SELECT rc.delete_rule
                FROM information_schema.referential_constraints rc
                WHERE rc.constraint_name = 'fk_habit_completions_habit'
                """, String.class);

        assertThat(deleteRule).isEqualTo("CASCADE");
    }

    @Test
    void usersEmailHasUniqueConstraint() {
        Integer uniqueIndexes = jdbc.queryForObject("""
                SELECT count(*)
                FROM pg_indexes
                WHERE tablename = 'users' AND indexdef ILIKE '%UNIQUE%email%'
                """, Integer.class);

        assertThat(uniqueIndexes).isGreaterThanOrEqualTo(1);
    }
}
