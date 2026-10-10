-- =====================================================================
-- V3 规范化时间列:业务表 TIMESTAMPTZ -> TIMESTAMP(WITHOUT TIME ZONE)
-- 背景:实体统一使用 java.time.LocalDateTime,PostgreSQL JDBC 不支持
-- 从 TIMESTAMPTZ 直接 getObject 为 LocalDateTime,导致业务查询报错
-- (Cannot convert the column of type TIMESTAMPTZ to java.time.LocalDateTime)。
-- Flowable(ACT_*)与 Flyway 历史表使用引擎自带的映射,保持原生类型不变。
-- =====================================================================
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_name, c.column_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'itsm'
          AND c.data_type    = 'timestamp with time zone'
          AND c.table_name NOT LIKE 'ACT\_%'
          AND c.table_name <> 'flyway_schema_history'
    LOOP
        EXECUTE format(
            'ALTER TABLE itsm.%I ALTER COLUMN %I TYPE TIMESTAMP WITHOUT TIME ZONE USING %I AT TIME ZONE %L',
            r.table_name, r.column_name, r.column_name, 'Asia/Shanghai');
    END LOOP;
END $$;

