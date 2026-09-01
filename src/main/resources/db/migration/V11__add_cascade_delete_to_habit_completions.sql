-- V7 creó fk_habit_completions_habit sin regla de borrado, así que Postgres usa
-- el default (NO ACTION): borrar un habit con al menos una fila en habit_completions
-- falla con violación de FK. deleteHabit() queda inutilizable para cualquier hábito
-- que se haya completado alguna vez.
--
-- El modelo mantiene Habit (definición) y HabitCompletion (evento) como entidades
-- separadas a propósito, y Habit no mapea la colección de completions, así que no
-- hay cascade JPA posible sin romper esa separación. La regla de borrado vive en la
-- BD, igual que el UNIQUE(habit_id, date) que ya está en V7.
ALTER TABLE habit_completions
    DROP CONSTRAINT fk_habit_completions_habit;

ALTER TABLE habit_completions
    ADD CONSTRAINT fk_habit_completions_habit
        FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE;
