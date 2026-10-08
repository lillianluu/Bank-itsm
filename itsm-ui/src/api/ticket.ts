/**
 * 工单域(M2)类型定义 + 原型内存数据仓库。
 *
 * 原型阶段:数据保存在内存中,刷新页面即重置;状态流转在此处做集中校验,
 * 与精简版设计文档 4.1 的统一状态机一致。
 * 对接后端时:将本文件内 store 的各方法替换为 http.get/post 调用即可,
 * 页面层无需改动(接口约定:/api/v1/tickets 系列)。
 */
import http from './http';

// ============ 类型定义 ============

/** 工单类型(四类统一主干) */
export type TicketType = 'INCIDENT' | 'REQUEST' | 'PROBLEM' | 'CHANGE';

/** 工单状态(精简版设计文档 4.1 状态机) */
export type TicketStatus = 'DRAFT' | 'PENDING' | 'ACCEPTED' | 'PROCESSING' | 'SUSPENDED' | 'RESOLVED' | 'CLOSED';

/** 优先级(P1 最高) */
export type Priority = 'P1' | 'P2' | 'P3' | 'P4';

/** 影响度 / 紧急度 */
export type Impact = 'I1' | 'I2' | 'I3';
export type Urgency = 'U1' | 'U2' | 'U3';

/** 时间线动作 */
export type TicketActionType =
  | 'create' | 'submit' | 'draft-save' | 'accept' | 'start' | 'assign' | 'suspend'
  | 'resume' | 'resolve' | 'close' | 'reopen' | 'cancel';

export interface TicketAction {
  id: number;
  type: TicketActionType;
  operator: string;
  comment: string;
  at: string;
}

export interface Ticket {
  id: string;
  no: string;
  type: TicketType;
  title: string;
  description: string;
  status: TicketStatus;
  priority: Priority;
  impact: Impact;
  urgency: Urgency;
  reporter: string;
  assignee: string | null;
  assigneeGroup: string;
  /** 参与者(协作处理/抄送,原型字段) */
  participants: string[];
  createdAt: string;
  updatedAt: string;
  slaResponseDeadline: string;
  slaResolveDeadline: string;
  actions: TicketAction[];
}

export interface TicketCreatePayload {
  type: TicketType;
  catalogItem?: string;
  title: string;
  description: string;
  impact: Impact;
  urgency: Urgency;
  priority: Priority;
  assigneeGroup: string;
  /** 保存为草稿(仅保留基础信息,未正式提交、不启动 SLA 展示) */
  draft?: boolean;
}

// ============ 展示用字典(对接后端后改为 /api/v1/dicts 拉取) ============

export const TYPE_LABEL: Record<TicketType, string> = {
  INCIDENT: '事件', REQUEST: '服务请求', PROBLEM: '问题', CHANGE: '变更'
};

export const TYPE_TAG: Record<TicketType, string> = {
  INCIDENT: 'danger', REQUEST: 'primary', PROBLEM: 'warning', CHANGE: 'success'
};

export const STATUS_LABEL: Record<TicketStatus, string> = {
  DRAFT: '草稿', PENDING: '待受理', ACCEPTED: '已受理', PROCESSING: '处理中',
  SUSPENDED: '已挂起', RESOLVED: '已解决', CLOSED: '已关闭'
};

export const STATUS_TAG: Record<TicketStatus, string> = {
  DRAFT: 'info', PENDING: 'info', ACCEPTED: 'primary', PROCESSING: 'warning',
  SUSPENDED: 'danger', RESOLVED: 'success', CLOSED: 'info'
};

export const PRIORITY_LABEL: Record<Priority, string> = {
  P1: 'P1 紧急', P2: 'P2 高', P3: 'P3 中', P4: 'P4 低'
};

export const PRIORITY_TAG: Record<Priority, string> = {
  P1: 'danger', P2: 'warning', P3: 'primary', P4: 'info'
};

export const IMPACT_LABEL: Record<Impact, string> = {
  I1: '单个用户', I2: '单个部门', I3: '全员/核心业务'
};

export const URGENCY_LABEL: Record<Urgency, string> = {
  U1: '低', U2: '中', U3: '高'
};

export const ACTION_LABEL: Record<TicketActionType, string> = {
  create: '提交', submit: '正式提交', 'draft-save': '保存草稿', accept: '受理', start: '开始处理', assign: '转派',
  suspend: '挂起', resume: '恢复处理', resolve: '提交解决方案',
  close: '确认关闭', reopen: '重开', cancel: '无效关闭'
};

/** 处理组(原型示例,后续接部门/组数据) */
export const HANDLE_GROUPS = ['服务台', '桌面支持组', '网络组', '系统组', '应用组'];

/** 服务目录(原型示例,后续接 catalog_item 表) */
export const CATALOG_TREE = [
  {
    value: 'software', label: '软件服务', children: [
      { value: 'software-install', label: '软件安装申请' },
      { value: 'software-perm', label: '权限开通申请' }
    ]
  },
  {
    value: 'hardware', label: '硬件服务', children: [
      { value: 'hardware-new', label: '新设备申领' },
      { value: 'hardware-repair', label: '设备维修' }
    ]
  },
  {
    value: 'account', label: '账号服务', children: [
      { value: 'account-new', label: '账号开通' },
      { value: 'account-reset', label: '密码重置' }
    ]
  }
];

/**
 * 优先级矩阵:影响度 × 紧急度(精简版设计文档 4.1)
 */
const PRIORITY_MATRIX: Record<string, Priority> = {
  'I1-U1': 'P4', 'I1-U2': 'P3', 'I1-U3': 'P2',
  'I2-U1': 'P3', 'I2-U2': 'P2', 'I2-U3': 'P1',
  'I3-U1': 'P2', 'I3-U2': 'P1', 'I3-U3': 'P1'
};

export function suggestPriority(impact: Impact, urgency: Urgency): Priority {
  return PRIORITY_MATRIX[`${impact}-${urgency}`] ?? 'P3';
}

/** SLA 预置策略(分钟),按「工单类型 × 优先级」后续可配置 */
const SLA_TABLE: Record<Priority, { response: number; resolve: number }> = {
  P1: { response: 15, resolve: 4 * 60 },
  P2: { response: 30, resolve: 8 * 60 },
  P3: { response: 120, resolve: 24 * 60 },
  P4: { response: 480, resolve: 72 * 60 }
};

/** 原型阶段的"当前登录用户"(M1 登录打通后从用户上下文取) */
export const CURRENT_USER = 'admin';

// ============ 状态机:合法流转 ============

const TRANSITIONS: Record<TicketActionType, { from: TicketStatus[]; to: TicketStatus | null }> = {
  create: { from: [], to: 'PENDING' },
  submit: { from: ['DRAFT'], to: 'PENDING' },
  'draft-save': { from: ['DRAFT'], to: 'DRAFT' },
  accept: { from: ['PENDING'], to: 'ACCEPTED' },
  start: { from: ['ACCEPTED'], to: 'PROCESSING' },
  assign: { from: ['PENDING', 'ACCEPTED', 'PROCESSING'], to: null }, // 转派不改变状态
  suspend: { from: ['PROCESSING'], to: 'SUSPENDED' },
  resume: { from: ['SUSPENDED'], to: 'PROCESSING' },
  resolve: { from: ['PROCESSING'], to: 'RESOLVED' },
  close: { from: ['RESOLVED'], to: 'CLOSED' },
  reopen: { from: ['RESOLVED'], to: 'PROCESSING' },
  cancel: { from: ['PENDING'], to: 'CLOSED' }
};

export function canAct(status: TicketStatus, action: TicketActionType): boolean {
  const t = TRANSITIONS[action];
  return t.from.includes(status);
}

// ============ 内存数据仓库(原型 mock) ============

let seq = 1000;
let actionSeq = 1;

const NO_PREFIX: Record<TicketType, string> = {
  INCIDENT: 'INC', REQUEST: 'REQ', PROBLEM: 'PRB', CHANGE: 'CHG'
};

function genNo(type: TicketType): string {
  const d = new Date();
  const ymd = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`;
  return `${NO_PREFIX[type]}-${ymd}-${String(++seq).padStart(4, '0')}`;
}

function hoursAgo(h: number): string {
  return new Date(Date.now() - h * 3600_000).toISOString();
}

function hoursLater(h: number): string {
  return new Date(Date.now() + h * 3600_000).toISOString();
}

function seed(type: TicketType, title: string, description: string, priority: Priority,
              impact: Impact, urgency: Urgency, status: TicketStatus,
              reporter: string, assignee: string | null, group: string,
              createdHoursAgo: number, resolveDeadlineHours: number): Ticket {
  const sla = SLA_TABLE[priority];
  const actions: TicketAction[] = [{
    id: actionSeq++, type: 'create', operator: reporter,
    comment: status === 'DRAFT' ? '保存草稿' : '提交工单', at: hoursAgo(createdHoursAgo)
  }];
  if (status !== 'PENDING' && status !== 'DRAFT') {
    actions.push({
      id: actionSeq++, type: 'accept', operator: assignee ?? '服务台',
      comment: '受理工单', at: hoursAgo(createdHoursAgo - 0.3 > 0 ? createdHoursAgo - 0.3 : 0.1)
    });
  }
  if (status === 'PROCESSING' || status === 'SUSPENDED' || status === 'RESOLVED' || status === 'CLOSED') {
    actions.push({ id: actionSeq++, type: 'start', operator: assignee ?? '工程师', comment: '开始处理', at: hoursAgo(Math.max(createdHoursAgo - 0.5, 0.1)) });
  }
  if (status === 'RESOLVED') {
    actions.push({ id: actionSeq++, type: 'resolve', operator: assignee ?? '工程师', comment: '已更换故障内存条,服务恢复', at: hoursAgo(Math.max(createdHoursAgo - 1, 0.1)) });
  }
  if (status === 'CLOSED') {
    actions.push({ id: actionSeq++, type: 'close', operator: reporter, comment: '问题确认解决,感谢处理', at: hoursAgo(Math.max(createdHoursAgo - 1.5, 0.1)) });
  }
  return {
    id: String(seq + 1),
    no: genNo(type),
    type, title, description, status, priority, impact, urgency,
    reporter, assignee, assigneeGroup: group,
    participants: [],
    createdAt: hoursAgo(createdHoursAgo),
    updatedAt: hoursAgo(Math.max(createdHoursAgo - 0.5, 0)),
    slaResponseDeadline: new Date(new Date(hoursAgo(createdHoursAgo)).getTime() + sla.response * 60_000).toISOString(),
    slaResolveDeadline: resolveDeadlineHours > 0 ? hoursLater(resolveDeadlineHours) : hoursLater(sla.resolve),
    actions
  };
}

const tickets: Ticket[] = [
  seed('INCIDENT', 'OA 系统无法登录', '上午 9 点起 OA 系统登录报 500 错误,影响全员办公。', 'P1', 'I3', 'U3', 'PROCESSING', '张三', '李工', '系统组', 2, 1.5),
  seed('INCIDENT', '会议室投影仪无信号', '三楼大会议室投影仪连接后无信号输出。', 'P3', 'I1', 'U2', 'PENDING', '王五', null, '桌面支持组', 5, 0),
  seed('REQUEST', '开通 GitLab 账号', '新入职同事需要开通 GitLab 账号及 devops 项目权限。', 'P3', 'I1', 'U2', 'ACCEPTED', '赵六', '钱工', '系统组', 8, 0),
  seed('INCIDENT', 'VPN 无法连接', '出差在外 VPN 拨号失败,提示认证超时。', 'P2', 'I2', 'U3', 'RESOLVED', '张三', '孙工', '网络组', 26, 0),
  seed('REQUEST', '申领一台显示器', '工位显示器损坏,申领新显示器一台。', 'P4', 'I1', 'U1', 'CLOSED', '周八', '吴工', '桌面支持组', 50, 0),
  seed('INCIDENT', '财务系统报表导出超时', '月末导出报表一直超时,无法完成月结。', 'P2', 'I2', 'U2', 'SUSPENDED', '郑九', 'admin', '应用组', 10, 0),
  seed('REQUEST', '密码重置申请', '域账号密码忘记,申请重置。', 'P3', 'I1', 'U2', 'PENDING', 'admin', null, '桌面支持组', 4, 0),
  seed('REQUEST', '笔记本内存扩容申请', '开发机内存不足,申请扩容至 32G(草稿,未正式提交)。', 'P4', 'I1', 'U1', 'DRAFT', 'admin', null, '桌面支持组', 20, 0)
];

// 原型:补充参与者数据(支撑「我参与的工单」视图)
tickets[0].participants = ['admin', '孙工'];
tickets[2].participants = ['admin'];

/** SLA 剩余毫秒数(负数即已超时) */
export function slaRemainingMs(t: Ticket): number {
  return new Date(t.slaResolveDeadline).getTime() - Date.now();
}

function fmt(no: string, at: string): string {
  return `${no}(${new Date(at).toLocaleString('zh-CN', { hour12: false })})`;
}

export const ticketStore = {
  /** 工单列表(原型:内存过滤;后端:GET /tickets?page=&size=&type=&status=&priority=&keyword=) */
  list(filter?: { type?: TicketType; status?: TicketStatus; priority?: Priority; keyword?: string }): Ticket[] {
    return tickets
      .filter((t) => (!filter?.type || t.type === filter.type)
        && (!filter?.status || t.status === filter.status)
        && (!filter?.priority || t.priority === filter.priority)
        && (!filter?.keyword || t.title.includes(filter.keyword) || t.no.includes(filter.keyword)))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  },

  /** 工单详情(后端:GET /tickets/{id}) */
  get(id: string): Ticket | undefined {
    return tickets.find((t) => t.id === id);
  },

  /** 新建(后端:POST /tickets) */
  create(p: TicketCreatePayload): Ticket {
    const sla = SLA_TABLE[p.priority];
    const now = new Date();
    const t: Ticket = {
      id: String(++seq),
      no: genNo(p.type),
      type: p.type,
      title: p.title,
      description: p.description,
      status: p.draft ? 'DRAFT' : 'PENDING',
      priority: p.priority,
      impact: p.impact,
      urgency: p.urgency,
      reporter: CURRENT_USER,
      assignee: null,
      assigneeGroup: p.assigneeGroup,
      participants: [],
      createdAt: now.toISOString(),
      updatedAt: now.toISOString(),
      slaResponseDeadline: new Date(now.getTime() + sla.response * 60_000).toISOString(),
      slaResolveDeadline: new Date(now.getTime() + sla.resolve * 60_000).toISOString(),
      actions: [{
        id: actionSeq++,
        type: 'create',
        operator: CURRENT_USER,
        comment: p.draft ? '保存草稿' : (p.catalogItem ? `通过服务目录「${p.catalogItem}」提交` : '提交工单'),
        at: now.toISOString()
      }]
    };
    tickets.unshift(t);
    return t;
  },

  /**
   * 状态流转(后端:POST /tickets/{id}/actions)。
   * 非法流转抛错,由页面 ElMessage 提示。
   */
  act(id: string, action: Exclude<TicketActionType, 'create'>, comment: string,
      newAssignee?: string): Ticket {
    const t = tickets.find((x) => x.id === id);
    if (!t) {
      throw new Error('工单不存在');
    }
    if (!canAct(t.status, action)) {
      throw new Error(`当前状态「${STATUS_LABEL[t.status]}」不允许执行「${ACTION_LABEL[action]}」`);
    }
    if (action === 'assign' && !newAssignee) {
      throw new Error('转派必须指定新的处理人');
    }
    const target = TRANSITIONS[action].to;
    if (target) {
      t.status = target;
    }
    if (action === 'assign' && newAssignee) {
      t.assignee = newAssignee;
      t.assigneeGroup = newAssignee;
    }
    const now = new Date().toISOString();
    t.updatedAt = now;
    t.actions.push({
      id: actionSeq++, type: action, operator: CURRENT_USER,
      comment: comment || ACTION_LABEL[action], at: now
    });
    return t;
  },

  /** 看板统计(后端:GET /dashboard/overview) */
  overview(): { todayNew: number; pending: number; processing: number; overdue: number; resolveRate: number } {
    const today = new Date().toDateString();
    return {
      todayNew: tickets.filter((t) => new Date(t.createdAt).toDateString() === today).length,
      pending: tickets.filter((t) => t.status === 'PENDING').length,
      processing: tickets.filter((t) => ['ACCEPTED', 'PROCESSING', 'SUSPENDED'].includes(t.status)).length,
      overdue: tickets.filter((t) => t.status !== 'CLOSED' && slaRemainingMs(t) < 0).length,
      resolveRate: Math.round(
        (tickets.filter((t) => t.status === 'RESOLVED' || t.status === 'CLOSED').length
          / Math.max(tickets.length, 1)) * 100)
    };
  },

  /**
   * 我的工单四类视图(后端对应:GET /tickets/my/{drafts|todo|submitted|involved})
   * - drafts:我的草稿(未正式提交,仅基础信息)
   * - todo:我的待办(分配给我且未关闭)
   * - submitted:我提交的(排除草稿)
   * - involved:我参与的(我是处理人或参与者,排除草稿与我提交的)
   */
  mine(): { drafts: Ticket[]; todo: Ticket[]; submitted: Ticket[]; involved: Ticket[] } {
    return {
      drafts: tickets.filter((t) => t.reporter === CURRENT_USER && t.status === 'DRAFT'),
      todo: tickets.filter((t) => t.assignee === CURRENT_USER
        && ['PENDING', 'ACCEPTED', 'PROCESSING', 'SUSPENDED'].includes(t.status)),
      submitted: tickets.filter((t) => t.reporter === CURRENT_USER && t.status !== 'DRAFT'),
      involved: tickets.filter((t) => t.status !== 'DRAFT' && t.reporter !== CURRENT_USER
        && (t.assignee === CURRENT_USER || t.participants.includes(CURRENT_USER)))
    };
  },

  /** 保存草稿修改(后端:PUT /tickets/{id}/draft) */
  saveDraft(id: string, p: TicketCreatePayload): Ticket {
    const t = tickets.find((x) => x.id === id);
    if (!t) {
      throw new Error('工单不存在');
    }
    if (t.status !== 'DRAFT') {
      throw new Error('仅草稿状态可编辑保存');
    }
    Object.assign(t, {
      type: p.type, title: p.title, description: p.description,
      impact: p.impact, urgency: p.urgency, priority: p.priority,
      assigneeGroup: p.assigneeGroup
    });
    const now = new Date().toISOString();
    t.updatedAt = now;
    t.actions.push({
      id: actionSeq++, type: 'draft-save', operator: CURRENT_USER,
      comment: '编辑草稿', at: now
    });
    return t;
  },

  /** 删除草稿(后端:DELETE /tickets/{id},仅草稿) */
  removeDraft(id: string): void {
    const t = tickets.find((x) => x.id === id);
    if (!t) {
      throw new Error('工单不存在');
    }
    if (t.status !== 'DRAFT') {
      throw new Error('仅草稿状态可删除');
    }
    tickets.splice(tickets.indexOf(t), 1);
  },

  /** 预留:示例说明对接后端后的真实调用形态(页面无需感知) */
  async _realApiSample(id: string): Promise<Ticket> {
    return http.get<never, Ticket>(`/tickets/${id}`);
  }
};

/** 供原型调试:格式化流转日志 */
export function debugLog(t: Ticket): void {
  t.actions.forEach((a) => console.debug(fmt(t.no, a.at), ACTION_LABEL[a.type], a.operator, a.comment));
}

