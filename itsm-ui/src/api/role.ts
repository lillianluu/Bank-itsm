/**
 * 系统管理 - 角色管理(真实后端接口,对接 itsm-module-platform)。
 *
 * 角色:  GET /roles  POST /roles  PUT /roles/{id}  DELETE /roles/{id}  PUT /roles/{id}/menus
 * 菜单树: GET /menus/tree(角色分配菜单的授权树,含按钮权限点)
 *
 * 删除说明:后端 DELETE /roles/{id} 为逻辑删除 —— 仅将 sys_role.deleted 标记为 true,
 * 同时清理 sys_role_menu 关联;内置 ADMIN 角色受代码保护,编码不可改、不可删除。
 */
import http from './http';

export type DataScope = 'SELF' | 'DEPT' | 'ALL';

export const DATA_SCOPE_LABEL: Record<DataScope, string> = {
  SELF: '仅本人', DEPT: '本部门', ALL: '全部数据'
};

/** 内置超管角色编码(代码层直通校验,前端禁改/禁删) */
export const BUILTIN_ADMIN = 'ADMIN';

/** 对齐后端 RoleVO */
export interface Role {
  id: number;
  name: string;
  code: string;
  dataScope: DataScope;
  status: 0 | 1;
  remark: string | null;
  /** 已授权菜单/权限点 id 集合(含作为半选父节点存库的目录 id) */
  menuIds: number[];
}

/** 对齐后端 RoleSaveCmd */
export interface RoleSaveCmd {
  name: string;
  code: string;
  dataScope: DataScope;
  status: 0 | 1;
  remark: string | null;
}

/** 对齐后端 MenuNode(管理端全量菜单树,含按钮权限点) */
export interface MenuTreeNode {
  id: number;
  parentId: number;
  name: string;
  type: 'M' | 'C' | 'F';
  path: string | null;
  component: string | null;
  perm: string | null;
  icon: string | null;
  sort: number;
  children: MenuTreeNode[];
}

/** 角色列表(每项含已授权的 menuIds,用于编辑与授权回显) */
export function listRoles(): Promise<Role[]> {
  return http.get<never, Role[]>('/roles');
}

/** 新增角色,返回新角色 id */
export function createRole(cmd: RoleSaveCmd): Promise<number> {
  return http.post<never, number>('/roles', cmd);
}

/** 修改角色 */
export function updateRole(id: number, cmd: RoleSaveCmd): Promise<void> {
  return http.put<never, void>(`/roles/${id}`, cmd);
}

/** 删除角色(逻辑删除;内置 ADMIN 会被后端拒绝) */
export function deleteRole(id: number): Promise<void> {
  return http.delete<never, void>(`/roles/${id}`);
}

/** 分配菜单/权限点(全量覆盖该角色的 sys_role_menu) */
export function assignRoleMenus(id: number, menuIds: number[]): Promise<void> {
  return http.put<never, void>(`/roles/${id}/menus`, { menuIds });
}

/** 管理端菜单树(角色授权用,目录/菜单/按钮全量) */
export function fetchMenuTree(): Promise<MenuTreeNode[]> {
  return http.get<never, MenuTreeNode[]>('/menus/tree');
}

