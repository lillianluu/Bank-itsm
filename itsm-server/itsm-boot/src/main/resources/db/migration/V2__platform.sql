-- =====================================================================
-- V2 平台基线:用户 / 部门 / 角色 / 菜单权限 + 种子数据(M1,精简版 3.1~3.2)
-- =====================================================================

-- 部门(树形,ancestors 存祖先链 "0,1,3")
CREATE TABLE sys_dept (
    id         BIGSERIAL PRIMARY KEY,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    ancestors  VARCHAR(256) NOT NULL DEFAULT '0',
    name       VARCHAR(64)  NOT NULL,
    sort       INT          NOT NULL DEFAULT 0,
    leader     VARCHAR(64),
    phone      VARCHAR(32),
    email      VARCHAR(128),
    status     SMALLINT     NOT NULL DEFAULT 1,          -- 1启用 0停用
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ,
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_sys_dept_parent ON sys_dept(parent_id);

-- 用户
CREATE TABLE sys_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    password      VARCHAR(100) NOT NULL,                  -- BCrypt
    nickname      VARCHAR(64)  NOT NULL,
    email         VARCHAR(128),
    phone         VARCHAR(32),
    dept_id       BIGINT,
    status        SMALLINT     NOT NULL DEFAULT 1,        -- 1启用 0停用
    last_login_at TIMESTAMPTZ,
    created_by    VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by    VARCHAR(64),
    updated_at    TIMESTAMPTZ,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user(username) WHERE deleted = FALSE;
CREATE INDEX idx_sys_user_dept ON sys_user(dept_id);

-- 角色(data_scope: SELF 本人 / DEPT 本部门 / ALL 全部)
CREATE TABLE sys_role (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(64) NOT NULL,
    code       VARCHAR(64) NOT NULL,
    sort       INT         NOT NULL DEFAULT 0,
    data_scope VARCHAR(16) NOT NULL DEFAULT 'SELF',
    status     SMALLINT    NOT NULL DEFAULT 1,
    remark     VARCHAR(256),
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uk_sys_role_code ON sys_role(code) WHERE deleted = FALSE;

-- 菜单与权限点(type: M目录 C页面 F按钮/接口权限点)
CREATE TABLE sys_menu (
    id         BIGSERIAL PRIMARY KEY,
    parent_id  BIGINT      NOT NULL DEFAULT 0,
    name       VARCHAR(64) NOT NULL,
    type       VARCHAR(1)  NOT NULL,
    path       VARCHAR(128),
    component  VARCHAR(128),
    perm       VARCHAR(128),
    icon       VARCHAR(64),
    sort       INT         NOT NULL DEFAULT 0,
    visible    SMALLINT    NOT NULL DEFAULT 1,
    status     SMALLINT    NOT NULL DEFAULT 1,
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_sys_menu_parent ON sys_menu(parent_id);

-- 关联表
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

-- ================= 种子数据 =================
INSERT INTO sys_dept (id, parent_id, ancestors, name, sort) VALUES (1, 0, '0', '总部', 1);

INSERT INTO sys_role (id, name, code, sort, data_scope, remark) VALUES
    (1, '系统管理员',   'ADMIN',      1, 'ALL',  '全部权限,代码内置放行'),
    (2, '服务台坐席',   'SEAT',       2, 'ALL',  '统一受理、分派'),
    (3, '处理组工程师', 'ENGINEER',   3, 'DEPT', '受理本组工单并处理'),
    (4, '员工',         'EMPLOYEE',   4, 'SELF', '提单、查看本人工单'),
    (5, '变更经理',     'CHANGE_MGR', 5, 'ALL',  '变更评估与审批'),
    (6, '管理者',       'MANAGER',    6, 'ALL',  '看板与报表只读');

-- 菜单:目录/页面
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perm, icon, sort) VALUES
    (1, 0, '工作台',   'M', '/workbench',      'workbench/index',    NULL,             'Odometer', 1),
    (2, 0, '服务台',   'M', '/itsm',           NULL,                 NULL,             'Monitor',  2),
    (3, 2, '工单管理', 'C', '/itsm/ticket',    'itsm/ticket/index',  'itsm:ticket:list', 'Tickets',  1),
    (4, 0, '系统管理', 'M', '/system',         NULL,                 NULL,             'Setting',  9),
    (5, 4, '用户管理', 'C', '/system/user',    'system/user/index',  'sys:user:list',  'User',     1),
    (6, 4, '部门管理', 'C', '/system/dept',    'system/dept/index',  'sys:dept:list',  'OfficeBuilding', 2),
    (7, 4, '角色管理', 'C', '/system/role',    'system/role/index',  'sys:role:list',  'UserFilled', 3),
    (8, 4, '菜单管理', 'C', '/system/menu',    'system/menu/index',  'sys:menu:list',  'Menu',     4);

-- 菜单:按钮/接口权限点
INSERT INTO sys_menu (id, parent_id, name, type, perm, sort) VALUES
    (101, 5, '用户新增',   'F', 'sys:user:add',        1),
    (102, 5, '用户编辑',   'F', 'sys:user:edit',       2),
    (103, 5, '用户删除',   'F', 'sys:user:delete',     3),
    (104, 5, '重置密码',   'F', 'sys:user:reset-pwd',   4),
    (105, 5, '分配角色',   'F', 'sys:user:assign-role', 5),
    (106, 6, '部门新增',   'F', 'sys:dept:add',        1),
    (107, 6, '部门编辑',   'F', 'sys:dept:edit',       2),
    (108, 6, '部门删除',   'F', 'sys:dept:delete',     3),
    (109, 7, '角色新增',   'F', 'sys:role:add',        1),
    (110, 7, '角色编辑',   'F', 'sys:role:edit',       2),
    (111, 7, '角色删除',   'F', 'sys:role:delete',     3),
    (112, 7, '分配菜单',   'F', 'sys:role:assign-menu', 4),
    (113, 8, '菜单新增',   'F', 'sys:menu:add',        1),
    (114, 8, '菜单编辑',   'F', 'sys:menu:edit',       2),
    (115, 8, '菜单删除',   'F', 'sys:menu:delete',     3);

-- 初始管理员:admin / admin123(BCrypt 散列)
INSERT INTO sys_user (id, username, password, nickname, dept_id, status) VALUES
    (1, 'admin', '$2a$10$r7udClZdmIqJWjYNkhDyL.tDhq70NvO2n4wW6UYKFm0G4awVBv7zG', '系统管理员', 1, 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- ADMIN 角色授予全部菜单(代码层另有 ADMIN 直通校验,此处保证数据完整)
INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu WHERE deleted = FALSE;

