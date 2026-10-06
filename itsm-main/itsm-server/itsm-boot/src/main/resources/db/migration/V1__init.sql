-- =====================================================================
-- ITSM V1.0 基线(骨架阶段最小表集)
-- 完整数据模型见 docs/产品设计文档.md 第 8 章,后续以增量脚本演进
-- =====================================================================

-- 系统参数
CREATE TABLE sys_config (
    id           BIGSERIAL PRIMARY KEY,
    config_key   VARCHAR(128)  NOT NULL UNIQUE,
    config_value VARCHAR(1024),
    remark       VARCHAR(512),
    created_by   VARCHAR(64),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by   VARCHAR(64),
    updated_at   TIMESTAMPTZ
);

-- 数据字典
CREATE TABLE sys_dict (
    id         BIGSERIAL PRIMARY KEY,
    dict_type  VARCHAR(64)   NOT NULL,
    code       VARCHAR(64)   NOT NULL,
    label      VARCHAR(128)  NOT NULL,
    extra      JSONB,
    sort       INT           NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ,
    UNIQUE (dict_type, code)
);

-- 统一全文检索(PG FTS 默认实现使用;ES 为可选增强)
CREATE TABLE search_index (
    id         BIGSERIAL PRIMARY KEY,
    index_name VARCHAR(64)  NOT NULL,
    ref_id     VARCHAR(64)  NOT NULL,
    title      VARCHAR(512),
    content    TEXT,
    extra      JSONB,
    tsv        TSVECTOR,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (index_name, ref_id)
);
CREATE INDEX idx_search_index_tsv ON search_index USING GIN (tsv);
CREATE INDEX idx_search_index_extra ON search_index USING GIN (extra);

-- 可选增强(需 DBA 权限,按需执行):
--   CREATE EXTENSION IF NOT EXISTS pg_trgm;
--   CREATE INDEX idx_search_index_title_trgm ON search_index USING GIN (title gin_trgm_ops);

-- 初始字典:优先级
INSERT INTO sys_dict (dict_type, code, label, sort) VALUES
    ('priority', 'P1', '紧急', 1),
    ('priority', 'P2', '高',   2),
    ('priority', 'P3', '中',   3),
    ('priority', 'P4', '低',   4);

-- 初始字典:工单渠道
INSERT INTO sys_dict (dict_type, code, label, sort) VALUES
    ('channel', 'PORTAL',  '自助门户', 1),
    ('channel', 'EMAIL',   '邮件',     2),
    ('channel', 'IM',      '即时消息', 3),
    ('channel', 'PHONE',   '电话',     4),
    ('channel', 'MONITOR', '监控告警', 5),
    ('channel', 'API',     'OpenAPI',  6);

