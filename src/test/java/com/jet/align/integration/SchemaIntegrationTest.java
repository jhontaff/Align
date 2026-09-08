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

        assertThat(applied).isGreaterThanOrEqualTo(18);
    }

    // ddl-auto=validate checks column existence and type, never nullability, so a
    // migration that fails to apply a NOT NULL (or forgets it) never breaks startup.
    // V17 tightened tasks.due_date; V16 added tasks.reminder_sent NOT NULL.
    @Test
    void tasksDueDateAndReminderSentAreNotNull() {
        assertThat(isNullable("tasks", "due_date")).isEqualTo("NO");
        assertThat(isNullable("tasks", "reminder_sent")).isEqualTo("NO");
    }

    private String isNullable(String table, String column) {
        return jdbc.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_name = ? AND column_name = ?
                """, String.class, table, column);
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
