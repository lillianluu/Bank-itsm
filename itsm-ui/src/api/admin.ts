/**
 * 系统管理(M1 后端对应的原型 mock):用户 / 部门 / 角色 / 菜单。
 *
 * 数据结构与后端 module-platform 的 DTO 对齐(UserVO / DeptNode / RoleVO / MenuNode),
 * 对接后端时把各 store 方法替换为 http 调用即可:
 *   GET  /users  POST /users  PUT /users/{id}  DELETE /users/{ids}
 *   PUT  /users/{id}/password  PUT /users/{id}/roles
 *   GET  /depts/tree  POST /depts  PUT /depts/{id}  DELETE /depts/{id}
 *   GET  /roles  POST /roles  PUT /roles/{id}  DELETE /roles/{id}  PUT /roles/{id}/menus
 *   GET  /menus/tree  POST /menus  PUT /menus/{id}  DELETE /menus/{id}
 */

// ================= 部门 =================

export interface Dept {
  id: number;
  parentId: number;
  ancestors: string;
  name: string;
  sort: number;
  leader: string | null;
  phone: string | null;
  email: string | null;
  status: 0 | 1;
}

export interface DeptNode extends Dept {
  children: DeptNode[];
}

let deptSeq = 100;
const depts: Dept[] = [
  { id: 1, parentId: 0, ancestors: '0', name: '总部', sort: 1, leader: null, phone: null, email: null, status: 1 },
  { id: 2, parentId: 1, ancestors: '0,1', name: '信息技术部', sort: 1, leader: '李总', phone: '13800000001', email: 'it@corp.com', status: 1 },
  { id: 3, parentId: 2, ancestors: '0,1,2', name: '基础设施组', sort: 1, leader: '李工', phone: '13800000002', email: null, status: 1 },
  { id: 4, parentId: 2, ancestors: '0,1,2', name: '应用运维组', sort: 2, leader: '孙工', phone: '13800000003', email: null, status: 1 },
  { id: 5, parentId: 2, ancestors: '0,1,2', name: '研发中心', sort: 3, leader: '王工', phone: null, email: null, status: 1 },
  { id: 6, parentId: 1, ancestors: '0,1', name: '行政人事部', sort: 2, leader: '赵姐', phone: null, email: 'hr@corp.com', status: 1 }
];

function buildDeptTree(list: Dept[]): DeptNode[] {
  const map = new Map<number, DeptNode>();
  list.forEach((d) => map.set(d.id, { ...d, children: [] }));
  const roots: DeptNode[] = [];
  map.forEach((node) => {
    const parent = node.parentId === 0 ? undefined : map.get(node.parentId);
    if (parent) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  });
  return roots;
}

export const deptStore = {
  list(): Dept[] {
    return [...depts];
  },
  tree(): DeptNode[] {
    return buildDeptTree(depts.slice().sort((a, b) => a.sort - b.sort || a.id - b.id));
  },
  create(p: Omit<Dept, 'id' | 'ancestors'>): void {
    const parent = depts.find((d) => d.id === p.parentId);
    if (p.parentId !== 0 && !parent) {
      throw new Error('上级部门不存在');
    }
    depts.push({
      ...p,
      id: ++deptSeq,
      ancestors: parent ? `${parent.ancestors},${parent.id}` : '0'
    });
  },
  update(id: number, p: Omit<Dept, 'id' | 'ancestors'>): void {
    const dept = depts.find((d) => d.id === id);
    if (!dept) {
      throw new Error('部门不存在');
    }
    if (p.parentId === id) {
      throw new Error('上级部门不能是自身');
    }
    Object.assign(dept, p);
  },
  remove(id: number): void {
    if (depts.some((d) => d.parentId === id)) {
      throw new Error('存在下级部门,无法删除');
    }
    if (id === 1) {
      throw new Error('根部门不可删除');
    }
    depts.splice(depts.findIndex((d) => d.id === id), 1);
  }
};

// ================= 角色 =================

export type DataScope = 'SELF' | 'DEPT' | 'ALL';

export const DATA_SCOPE_LABEL: Record<DataScope, string> = {
  SELF: '仅本人', DEPT: '本部门', ALL: '全部数据'
};

export interface Role {
  id: number;
  name: string;
  code: string;
  dataScope: DataScope;
  status: 0 | 1;
  remark: string | null;
  menuIds: number[];
}

export const BUILTIN_ADMIN = 'ADMIN';

let roleSeq = 100;
const roles: Role[] = [
  { id: 1, name: '系统管理员', code: 'ADMIN', dataScope: 'ALL', status: 1, remark: '全部权限,代码内置放行', menuIds: [1, 2, 3, 4, 5, 21, 22, 221, 222, 223, 224, 31, 32, 33, 34, 35, 311, 312, 313, 314, 315, 321, 322, 323, 331, 332, 333, 334, 341, 342, 343, 351, 352, 353, 354] },
  { id: 2, name: '服务台坐席', code: 'SEAT', dataScope: 'ALL', status: 1, remark: '统一受理、分派', menuIds: [1, 2, 21, 22, 221, 222, 223, 224] },
  { id: 3, name: '处理组工程师', code: 'ENGINEER', dataScope: 'DEPT', status: 1, remark: '受理本组工单并处理', menuIds: [1, 2, 21, 22, 221, 222, 223, 224] },
  { id: 4, name: '员工', code: 'EMPLOYEE', dataScope: 'SELF', status: 1, remark: '提单、查看本人工单', menuIds: [1, 22, 221, 222, 223, 224] },
  { id: 5, name: '管理者', code: 'MANAGER', dataScope: 'ALL', status: 1, remark: '看板与报表只读', menuIds: [1] }
];

export const roleStore = {
  list(): Role[] {
    return [...roles].sort((a, b) => a.id - b.id);
  },
  create(p: Omit<Role, 'id' | 'menuIds'>): void {
    if (roles.some((r) => r.code === p.code)) {
      throw new Error(`角色编码已存在: ${p.code}`);
    }
    roles.push({ ...p, id: ++roleSeq, menuIds: [] });
  },
  update(id: number, p: Omit<Role, 'id' | 'menuIds'>): void {
    const role = roles.find((r) => r.id === id);
    if (!role) {
      throw new Error('角色不存在');
    }
    if (role.code === BUILTIN_ADMIN && p.code !== role.code) {
      throw new Error('内置角色编码不可修改');
    }
    if (roles.some((r) => r.code === p.code && r.id !== id)) {
      throw new Error(`角色编码已存在: ${p.code}`);
    }
    Object.assign(role, p);
  },
  remove(id: number): void {
    const role = roles.find((r) => r.id === id);
    if (!role) {
      throw new Error('角色不存在');
    }
    if (role.code === BUILTIN_ADMIN) {
      throw new Error('内置角色不可删除');
    }
    roles.splice(roles.indexOf(role), 1);
  },
  assignMenus(id: number, menuIds: number[]): void {
    const role = roles.find((r) => r.id === id);
    if (!role) {
      throw new Error('角色不存在');
    }
    role.menuIds = [...new Set(menuIds)];
  }
};

// ================= 用户 =================

export interface User {
  id: number;
  username: string;
  nickname: string;
  deptId: number | null;
  email: string | null;
  phone: string | null;
  status: 0 | 1;
  roleIds: number[];
  createdAt: string;
}

export interface UserFilter {
  keyword?: string;
  deptId?: number;
  status?: 0 | 1;
}

let userSeq = 100;
const users: User[] = [
  { id: 1, username: 'admin', nickname: '系统管理员', deptId: 2, email: 'admin@corp.com', phone: '13800000000', status: 1, roleIds: [1], createdAt: '2026-09-01T09:00:00Z' },
  { id: 2, username: 'zhangsan', nickname: '张三', deptId: 5, email: 'zhangsan@corp.com', phone: null, status: 1, roleIds: [4], createdAt: '2026-09-02T09:00:00Z' },
  { id: 3, username: 'ligong', nickname: '李工', deptId: 3, email: null, phone: '13800000002', status: 1, roleIds: [3], createdAt: '2026-09-03T09:00:00Z' },
  { id: 4, username: 'sungong', nickname: '孙工', deptId: 4, email: null, phone: '13800000003', status: 1, roleIds: [3], createdAt: '2026-09-03T09:30:00Z' },
  { id: 5, username: 'wangwu', nickname: '王五', deptId: 6, email: 'wangwu@corp.com', phone: null, status: 1, roleIds: [4], createdAt: '2026-09-05T09:00:00Z' },
  { id: 6, username: 'zhaoliu', nickname: '赵六', deptId: 3, email: null, phone: null, status: 1, roleIds: [2], createdAt: '2026-09-06T09:00:00Z' },
  { id: 7, username: 'zhouba', nickname: '周八', deptId: 4, email: null, phone: null, status: 0, roleIds: [3], createdAt: '2026-09-08T09:00:00Z' }
];

export const userStore = {
  list(filter?: UserFilter): User[] {
    return users
      .filter((u) => (!filter?.keyword
        || u.username.includes(filter.keyword) || u.nickname.includes(filter.keyword))
        && (!filter?.deptId || u.deptId === filter.deptId)
        && (filter?.status === undefined || u.status === filter.status))
      .sort((a, b) => b.id - a.id);
  },
  create(p: Omit<User, 'id' | 'createdAt'>, password: string): void {
    if (users.some((u) => u.username === p.username)) {
      throw new Error(`用户名已存在: ${p.username}`);
    }
    if (password.length < 6) {
      throw new Error('密码长度至少 6 位');
    }
    users.push({ ...p, id: ++userSeq, createdAt: new Date().toISOString() });
  },
  update(id: number, p: Omit<User, 'id' | 'createdAt' | 'username'>): void {
    const user = users.find((u) => u.id === id);
    if (!user) {
      throw new Error('用户不存在');
    }
    Object.assign(user, p);
  },
  remove(ids: number[]): void {
    if (ids.includes(1)) {
      throw new Error('不能删除内置管理员');
    }
    ids.forEach((id) => {
      const i = users.findIndex((u) => u.id === id);
      if (i >= 0) {
        users.splice(i, 1);
      }
    });
  },
  resetPassword(id: number, newPassword: string): void {
    if (newPassword.length < 6) {
      throw new Error('密码长度至少 6 位');
    }
    if (!users.some((u) => u.id === id)) {
      throw new Error('用户不存在');
    }
    // 原型仅模拟成功;真实实现:BCrypt 加密后 PUT /users/{id}/password
  }
};

// ================= 菜单 =================

export type MenuType = 'M' | 'C' | 'F';

export const MENU_TYPE_LABEL: Record<MenuType, string> = {
  M: '目录', C: '菜单', F: '按钮'
};

export const MENU_TYPE_TAG: Record<MenuType, string> = {
  M: 'primary', C: 'success', F: 'warning'
};

export interface MenuItem {
  id: number;
  parentId: number;
  name: string;
  type: MenuType;
  path: string | null;
  component: string | null;
  perm: string | null;
  icon: string | null;
  sort: number;
  visible: 0 | 1;
  status: 0 | 1;
}

export interface MenuNode extends MenuItem {
  children: MenuNode[];
}

let menuSeq = 1000;
const menus: MenuItem[] = [
  { id: 1, parentId: 0, name: '工作台', type: 'M', path: '/workbench', component: 'workbench/index', perm: null, icon: 'Odometer', sort: 1, visible: 1, status: 1 },
  { id: 2, parentId: 0, name: '工单管理', type: 'M', path: '/tickets', component: null, perm: null, icon: 'Tickets', sort: 2, visible: 1, status: 1 },
  { id: 21, parentId: 2, name: '工单列表', type: 'C', path: '/tickets', component: 'ticket/index', perm: 'itsm:ticket:list', icon: null, sort: 1, visible: 1, status: 1 },
  { id: 22, parentId: 2, name: '我的工单', type: 'M', path: '/my-tickets', component: null, perm: null, icon: 'Document', sort: 2, visible: 1, status: 1 },
  { id: 221, parentId: 22, name: '我的草稿', type: 'C', path: '/my-tickets/drafts', component: 'ticket/my-drafts', perm: null, icon: null, sort: 1, visible: 1, status: 1 },
  { id: 222, parentId: 22, name: '我的待办', type: 'C', path: '/my-tickets/todo', component: 'ticket/my-todo', perm: null, icon: null, sort: 2, visible: 1, status: 1 },
  { id: 223, parentId: 22, name: '我提交的工单', type: 'C', path: '/my-tickets/submitted', component: 'ticket/my-submitted', perm: null, icon: null, sort: 3, visible: 1, status: 1 },
  { id: 224, parentId: 22, name: '我参与的工单', type: 'C', path: '/my-tickets/participated', component: 'ticket/my-participated', perm: null, icon: null, sort: 4, visible: 1, status: 1 },
  { id: 3, parentId: 0, name: '系统管理', type: 'M', path: '/system', component: null, perm: null, icon: 'Setting', sort: 9, visible: 1, status: 1 },
  { id: 31, parentId: 3, name: '用户管理', type: 'C', path: '/system/user', component: 'system/user/index', perm: 'sys:user:list', icon: 'User', sort: 1, visible: 1, status: 1 },
  { id: 311, parentId: 31, name: '用户新增', type: 'F', path: null, component: null, perm: 'sys:user:add', icon: null, sort: 1, visible: 1, status: 1 },
  { id: 312, parentId: 31, name: '用户编辑', type: 'F', path: null, component: null, perm: 'sys:user:edit', icon: null, sort: 2, visible: 1, status: 1 },
  { id: 313, parentId: 31, name: '用户删除', type: 'F', path: null, component: null, perm: 'sys:user:delete', icon: null, sort: 3, visible: 1, status: 1 },
  { id: 314, parentId: 31, name: '重置密码', type: 'F', path: null, component: null, perm: 'sys:user:reset-pwd', icon: null, sort: 4, visible: 1, status: 1 },
  { id: 315, parentId: 31, name: '分配角色', type: 'F', path: null, component: null, perm: 'sys:user:assign-role', icon: null, sort: 5, visible: 1, status: 1 },
  { id: 32, parentId: 3, name: '部门管理', type: 'C', path: '/system/dept', component: 'system/dept/index', perm: 'sys:dept:list', icon: 'OfficeBuilding', sort: 2, visible: 1, status: 1 },
  { id: 321, parentId: 32, name: '部门新增', type: 'F', path: null, component: null, perm: 'sys:dept:add', icon: null, sort: 1, visible: 1, status: 1 },
  { id: 322, parentId: 32, name: '部门编辑', type: 'F', path: null, component: null, perm: 'sys:dept:edit', icon: null, sort: 2, visible: 1, status: 1 },
  { id: 323, parentId: 32, name: '部门删除', type: 'F', path: null, component: null, perm: 'sys:dept:delete', icon: null, sort: 3, visible: 1, status: 1 },
  { id: 33, parentId: 3, name: '角色管理', type: 'C', path: '/system/role', component: 'system/role/index', perm: 'sys:role:list', icon: 'UserFilled', sort: 3, visible: 1, status: 1 },
  { id: 331, parentId: 33, name: '角色新增', type: 'F', path: null, component: null, perm: 'sys:role:add', icon: null, sort: 1, visible: 1, status: 1 },
  { id: 332, parentId: 33, name: '角色编辑', type: 'F', path: null, component: null, perm: 'sys:role:edit', icon: null, sort: 2, visible: 1, status: 1 },
  { id: 333, parentId: 33, name: '角色删除', type: 'F', path: null, component: null, perm: 'sys:role:delete', icon: null, sort: 3, visible: 1, status: 1 },
  { id: 334, parentId: 33, name: '分配菜单', type: 'F', path: null, component: null, perm: 'sys:role:assign-menu', icon: null, sort: 4, visible: 1, status: 1 },
  { id: 34, parentId: 3, name: '菜单管理', type: 'C', path: '/system/menu', component: 'system/menu/index', perm: 'sys:menu:list', icon: 'Menu', sort: 4, visible: 1, status: 1 },
  { id: 341, parentId: 34, name: '菜单新增', type: 'F', path: null, component: null, perm: 'sys:menu:add', icon: null, sort: 1, visible: 1, status: 1 },
  { id: 342, parentId: 34, name: '菜单编辑', type: 'F', path: null, component: null, perm: 'sys:menu:edit', icon: null, sort: 2, visible: 1, status: 1 },
  { id: 343, parentId: 34, name: '菜单删除', type: 'F', path: null, component: null, perm: 'sys:menu:delete', icon: null, sort: 3, visible: 1, status: 1 },
  { id: 35, parentId: 3, name: '流程管理', type: 'C', path: '/system/process', component: 'system/process/index', perm: 'sys:process:list', icon: 'Share', sort: 5, visible: 1, status: 1 },
  { id: 351, parentId: 35, name: '流程新增', type: 'F', path: null, component: null, perm: 'sys:process:add', icon: null, sort: 1, visible: 1, status: 1 },
  { id: 352, parentId: 35, name: '流程编辑', type: 'F', path: null, component: null, perm: 'sys:process:edit', icon: null, sort: 2, visible: 1, status: 1 },
  { id: 353, parentId: 35, name: '流程发布', type: 'F', path: null, component: null, perm: 'sys:process:publish', icon: null, sort: 3, visible: 1, status: 1 },
  { id: 354, parentId: 35, name: '流程删除', type: 'F', path: null, component: null, perm: 'sys:process:delete', icon: null, sort: 4, visible: 1, status: 1 }
];

function buildMenuTree(list: MenuItem[]): MenuNode[] {
  const map = new Map<number, MenuNode>();
  list.forEach((m) => map.set(m.id, { ...m, children: [] }));
  const roots: MenuNode[] = [];
  map.forEach((node) => {
    const parent = node.parentId === 0 ? undefined : map.get(node.parentId);
    if (parent) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  });
  const bySort = (a: MenuNode, b: MenuNode) => a.sort - b.sort || a.id - b.id;
  roots.sort(bySort);
  map.forEach((n) => n.children.sort(bySort));
  return roots;
}

export const menuStore = {
  list(): MenuItem[] {
    return [...menus];
  },
  tree(): MenuNode[] {
    return buildMenuTree(menus);
  },
  /** 角色授权树:仅目录/菜单(不含按钮) */
  navTree(): MenuNode[] {
    return buildMenuTree(menus.filter((m) => m.type !== 'F'));
  },
  create(p: Omit<MenuItem, 'id'>): void {
    if (p.parentId !== 0 && !menus.some((m) => m.id === p.parentId)) {
      throw new Error('上级菜单不存在');
    }
    menus.push({ ...p, id: ++menuSeq });
  },
  update(id: number, p: Omit<MenuItem, 'id'>): void {
    const menu = menus.find((m) => m.id === id);
    if (!menu) {
      throw new Error('菜单不存在');
    }
    if (p.parentId === id) {
      throw new Error('上级菜单不能是自身');
    }
    Object.assign(menu, p);
  },
  remove(id: number): void {
    if (menus.some((m) => m.parentId === id)) {
      throw new Error('存在下级菜单,无法删除');
    }
    menus.splice(menus.findIndex((m) => m.id === id), 1);
  }
};

