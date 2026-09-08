UPDATE tasks SET due_date = CURRENT_DATE WHERE due_date IS NULL;

ALTER TABLE tasks
    ALTER COLUMN due_date SET NOT NULL;
