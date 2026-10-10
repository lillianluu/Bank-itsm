-- Normalize names before enforcing uniqueness; existing duplicates require manual resolution.
UPDATE sys_dept SET name = btrim(name) WHERE NOT deleted;
CREATE UNIQUE INDEX uk_sys_dept_parent_name_active
    ON sys_dept (parent_id, name) WHERE deleted = FALSE;
