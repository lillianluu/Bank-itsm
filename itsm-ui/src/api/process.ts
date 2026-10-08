/**
 * 流程管理(原型):图结构的流程定义(节点 + 连线),支持串行/并行/条件分支。
 *
 * 模型说明(与 Flowable 概念对应):
 * - TASK / APPROVAL     → UserTask(处理/审批)
 * - GATEWAY(EXCLUSIVE)  → 排他网关:出边带 condition 条件表达式,仅走一条
 * - GATEWAY(PARALLEL)   → 并行网关:所有出边同时激活(汇聚 = 多条入边指向同一节点)
 * - 串行                → 节点顺序连线
 *
 * 对接后端时:GET/POST/PUT /process-defs……;保存编排后由服务端生成 BPMN XML 并部署 Flowable。
 */
import type {TicketType} from './ticket';

// ============ 类型定义 ============

/** 节点类型:开始 / 处理任务 / 审批任务 / 网关 / 结束 */
export type ProcessNodeType = 'START' | 'TASK' | 'APPROVAL' | 'GATEWAY' | 'END';

/** 网关类型:排他(条件分支) / 并行(同时推进) */
export type GatewayType = 'EXCLUSIVE' | 'PARALLEL';

/** 处理人类型:发起人 / 指定用户 / 指定角色 / 处理组 */
export type AssigneeType = 'INITIATOR' | 'USER' | 'ROLE' | 'GROUP';

export interface ProcessNode {
  id: string;
  name: string;
  type: ProcessNodeType;
  /** 仅 GATEWAY 有效 */
  gatewayType: GatewayType | null;
  assigneeType: AssigneeType | null;
  assigneeValue: string | null;
  /** 节点级 SLA(小时) */
  slaHours: number | null;
  /** 画布坐标(持久化布局) */
  x: number;
  y: number;
}

export interface ProcessEdge {
  id: string;
  source: string;
  target: string;
  /** 连线标签(如 同意/驳回) */
  label: string | null;
  /** 条件表达式(排他网关出边,如 ${impact} == 'I3') */
  condition: string | null;
}

/** 流程状态:草稿 / 已发布 / 已停用 */
export type ProcessStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED';

export interface ProcessDef {
  id: number;
  /** 流程标识(全局唯一,对应 Flowable processDefinitionKey) */
  key: string;
  name: string;
  version: number;
  status: ProcessStatus;
  /** 绑定工单类型 */
  ticketType: TicketType;
  remark: string | null;
  updatedAt: string;
  nodes: ProcessNode[];
  edges: ProcessEdge[];
}

export interface ProcessGraph {
  nodes: ProcessNode[];
  edges: ProcessEdge[];
}

// ============ 展示用字典 ============

export const NODE_TYPE_LABEL: Record<ProcessNodeType, string> = {
  START: '开始', TASK: '处理任务', APPROVAL: '审批任务', GATEWAY: '网关', END: '结束'
};

export const NODE_TYPE_TAG: Record<ProcessNodeType, string> = {
  START: 'info', TASK: 'primary', APPROVAL: 'warning', GATEWAY: 'danger', END: 'info'
};

export const GATEWAY_TYPE_LABEL: Record<GatewayType, string> = {
  EXCLUSIVE: '排他网关(条件分支)', PARALLEL: '并行网关(同时推进)'
};

export const GATEWAY_TYPE_SHORT: Record<GatewayType, string> = {
  EXCLUSIVE: '排他', PARALLEL: '并行'
};

export const ASSIGNEE_TYPE_LABEL: Record<AssigneeType, string> = {
  INITIATOR: '发起人', USER: '指定用户', ROLE: '指定角色', GROUP: '处理组'
};

export const PROCESS_STATUS_LABEL: Record<ProcessStatus, string> = {
  DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用'
};

export const PROCESS_STATUS_TAG: Record<ProcessStatus, string> = {
  DRAFT: 'info', PUBLISHED: 'success', DISABLED: 'danger'
};

/** 处理角色候选(原型字典,后续接 sys_role) */
export const PROCESS_ROLE_OPTIONS = ['服务台坐席', '处理组工程师', '部门经理', '系统管理员'];

// ============ 内存数据仓库(原型 mock) ============

let seq = 100;
let nodeSeq = 10;
let edgeSeq = 10;

function nid(): string {
  return `node_${++nodeSeq}`;
}

function eid(): string {
  return `edge_${++edgeSeq}`;
}

function hoursAgo(h: number): string {
  return new Date(Date.now() - h * 3600_000).toISOString();
}

function startNode(): ProcessNode {
  return { id: 'start', name: '开始', type: 'START', gatewayType: null, assigneeType: null, assigneeValue: null, slaHours: null, x: 260, y: 0 };
}

function endNode(y: number): ProcessNode {
  return { id: 'end', name: '结束', type: 'END', gatewayType: null, assigneeType: null, assigneeValue: null, slaHours: null, x: 260, y };
}

/** 线性串行链:节点自动纵向排布并连线 */
function chain(steps: Array<Pick<ProcessNode, 'name' | 'type' | 'assigneeType' | 'assigneeValue' | 'slaHours' | 'gatewayType'>>): ProcessGraph {
  const nodes: ProcessNode[] = [startNode()];
  steps.forEach((s, i) => {
    nodes.push({ id: nid(), x: 260, y: (i + 1) * 100, ...s });
  });
  nodes.push(endNode((steps.length + 1) * 100));
  const edges: ProcessEdge[] = [];
  for (let i = 0; i < nodes.length - 1; i++) {
    edges.push({ id: eid(), source: nodes[i].id, target: nodes[i + 1].id, label: null, condition: null });
  }
  return { nodes, edges };
}

const processDefs: ProcessDef[] = [
  {
    id: 1, key: 'ticket_incident', name: '事件处理流程', version: 3, status: 'PUBLISHED',
    ticketType: 'INCIDENT', remark: '标准事件:受理 → 一线处理 → 主管审批 → 关闭', updatedAt: hoursAgo(30),
    ...chain([
      { name: '服务台受理', type: 'TASK', gatewayType: null, assigneeType: 'ROLE', assigneeValue: '服务台坐席', slaHours: 0.5 },
      { name: '一线处理', type: 'TASK', gatewayType: null, assigneeType: 'GROUP', assigneeValue: '桌面支持组', slaHours: 4 },
      { name: '主管审批', type: 'APPROVAL', gatewayType: null, assigneeType: 'ROLE', assigneeValue: '部门经理', slaHours: 2 }
    ])
  },
  (() => {
    // 服务请求:排他网关条件分支(P1 特批直通 / 常规走审批)
    const g = chain([
      { name: '服务台受理', type: 'TASK', gatewayType: null, assigneeType: 'ROLE', assigneeValue: '服务台坐席', slaHours: 1 },
      { name: '组长审批', type: 'APPROVAL', gatewayType: null, assigneeType: 'ROLE', assigneeValue: '部门经理', slaHours: 8 },
      { name: '二线处理', type: 'TASK', gatewayType: null, assigneeType: 'GROUP', assigneeValue: '系统组', slaHours: 24 }
    ]);
    const accept = g.nodes[1];
    const approval = g.nodes[2];
    const handle = g.nodes[3];
    const gw: ProcessNode = {
      id: nid(), name: '是否需审批', type: 'GATEWAY', gatewayType: 'EXCLUSIVE',
      assigneeType: null, assigneeValue: null, slaHours: null, x: 260, y: 200
    };
    // 受理 → 网关 →(常规)审批 → 处理;网关 →(P1 特批)处理
    g.edges = g.edges.filter((e) => !(e.source === accept.id));
    g.nodes.splice(2, 0, gw);
    g.edges.push(
      { id: eid(), source: accept.id, target: gw.id, label: null, condition: null },
      { id: eid(), source: gw.id, target: approval.id, label: '常规', condition: "${urgency} != 'U3'" },
      { id: eid(), source: gw.id, target: handle.id, label: '特批直通', condition: "${urgency} == 'U3'" }
    );
    return { id: 2, key: 'ticket_request', name: '服务请求审批流程', version: 1, status: 'PUBLISHED',
      ticketType: 'REQUEST', remark: '含排他网关:紧急请求特批直通,常规请求走审批', updatedAt: hoursAgo(80),
      nodes: g.nodes, edges: g.edges };
  })(),
  (() => {
    // 变更评审:并行网关(CAB 审批 与 安全评估 同时推进,汇聚到实施)
    const g = chain([
      { name: '变更经理评估', type: 'APPROVAL', gatewayType: null, assigneeType: 'ROLE', assigneeValue: '部门经理', slaHours: 4 },
      { name: '变更实施', type: 'TASK', gatewayType: null, assigneeType: 'GROUP', assigneeValue: '系统组', slaHours: 72 }
    ]);
    const assess = g.nodes[1];
    const impl = g.nodes[2];
    const gw: ProcessNode = {
      id: nid(), name: '并行评审', type: 'GATEWAY', gatewayType: 'PARALLEL',
      assigneeType: null, assigneeValue: null, slaHours: null, x: 260, y: 200
    };
    const cab: ProcessNode = {
      id: nid(), name: 'CAB 审批', type: 'APPROVAL', gatewayType: null,
      assigneeType: 'USER', assigneeValue: 'admin', slaHours: 24, x: 140, y: 300
    };
    const sec: ProcessNode = {
      id: nid(), name: '安全评估', type: 'APPROVAL', gatewayType: null,
      assigneeType: 'ROLE', assigneeValue: '系统管理员', slaHours: 24, x: 400, y: 300
    };
    g.edges = g.edges.filter((e) => !(e.source === assess.id));
    g.nodes.splice(2, 0, cab, sec, gw);
    g.nodes = g.nodes.map((n) => (n.id === 'end' ? { ...n, y: 420 } : n));
    g.edges.push(
      { id: eid(), source: assess.id, target: gw.id, label: null, condition: null },
      { id: eid(), source: gw.id, target: cab.id, label: '分支一', condition: null },
      { id: eid(), source: gw.id, target: sec.id, label: '分支二', condition: null },
      { id: eid(), source: cab.id, target: impl.id, label: null, condition: null },
      { id: eid(), source: sec.id, target: impl.id, label: null, condition: null }
    );
    return { id: 3, key: 'ticket_change', name: '变更评审流程', version: 2, status: 'DISABLED',
      ticketType: 'CHANGE', remark: '含并行网关:CAB 审批与安全评估并行推进,汇聚后实施', updatedAt: hoursAgo(200),
      nodes: g.nodes, edges: g.edges };
  })()
];

function find(id: number): ProcessDef | undefined {
  return processDefs.find((d) => d.id === id);
}

function touch(def: ProcessDef): void {
  def.updatedAt = new Date().toISOString();
}

function defaultGraph(): ProcessGraph {
  return chain([
    { name: '服务台受理', type: 'TASK', gatewayType: null, assigneeType: 'ROLE', assigneeValue: '服务台坐席', slaHours: null }
  ]);
}

/** 编排校验(保存与发布前调用),不合法抛错 */
function validate(graph: ProcessGraph): void {
  const { nodes, edges } = graph;
  if (!nodes.some((n) => n.type === 'START')) {
    throw new Error('流程缺少「开始」节点');
  }
  if (nodes.filter((n) => n.type === 'START').length > 1) {
    throw new Error('「开始」节点只能有一个');
  }
  if (!nodes.some((n) => n.type === 'END')) {
    throw new Error('流程缺少「结束」节点');
  }
  if (!nodes.some((n) => n.type === 'TASK' || n.type === 'APPROVAL')) {
    throw new Error('流程至少需要一个处理/审批任务节点');
  }
  const ids = new Set(nodes.map((n) => n.id));
  if (edges.some((e) => !ids.has(e.source) || !ids.has(e.target))) {
    throw new Error('存在连线指向不存在的节点,请检查');
  }
  if (edges.some((e) => e.source === e.target)) {
    throw new Error('不允许节点自连接');
  }
  for (const n of nodes) {
    const inDeg = edges.filter((e) => e.target === n.id).length;
    const outDeg = edges.filter((e) => e.source === n.id).length;
    if (n.type === 'START' && inDeg > 0) {
      throw new Error(`「${n.name}」为开始节点,不能有入边`);
    }
    if (n.type === 'END' && outDeg > 0) {
      throw new Error(`「${n.name}」为结束节点,不能有出边`);
    }
    if (n.type !== 'START' && inDeg === 0) {
      throw new Error(`「${n.name}」没有入边,无法到达`);
    }
    if (n.type !== 'END' && outDeg === 0) {
      throw new Error(`「${n.name}」没有出边,流程会中断`);
    }
    if (n.type === 'GATEWAY' && n.gatewayType === 'EXCLUSIVE' && outDeg < 2) {
      throw new Error(`排他网关「${n.name}」至少需要 2 条出边(条件分支)`);
    }
    if (n.type === 'GATEWAY' && n.gatewayType === 'PARALLEL' && outDeg < 2) {
      throw new Error(`并行网关「${n.name}」至少需要 2 条出边(并行分支)`);
    }
  }
}

export interface ProcessSavePayload {
  name: string;
  ticketType: TicketType;
  remark: string | null;
}

export const processStore = {
  list(): ProcessDef[] {
    return [...processDefs].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  },

  /** 新建(草稿,v0;发布后 version +1) */
  create(p: { key: string; name: string; ticketType: TicketType; remark: string | null }): ProcessDef {
    const key = p.key.trim();
    if (!/^[a-z][a-z0-9_]{2,31}$/.test(key)) {
      throw new Error('流程标识需以小写字母开头,仅含小写字母/数字/下划线,3-32 位');
    }
    if (processDefs.some((d) => d.key === key)) {
      throw new Error(`流程标识已存在: ${key}`);
    }
    const g = defaultGraph();
    const def: ProcessDef = {
      id: ++seq, key, name: p.name.trim(), version: 0, status: 'DRAFT',
      ticketType: p.ticketType, remark: p.remark,
      updatedAt: new Date().toISOString(), nodes: g.nodes, edges: g.edges
    };
    processDefs.unshift(def);
    return def;
  },

  /** 修改基本信息(key 不可改) */
  updateInfo(id: number, p: ProcessSavePayload): void {
    const def = find(id);
    if (!def) {
      throw new Error('流程不存在');
    }
    def.name = p.name.trim();
    def.ticketType = p.ticketType;
    def.remark = p.remark;
    touch(def);
  },

  /**
   * 保存节点编排(图结构)。
   * 已发布流程被修改后自动转回「草稿」,需重新发布(与 Flowable 重新部署语义一致)。
   */
  updateNodes(id: number, graph: ProcessGraph): void {
    const def = find(id);
    if (!def) {
      throw new Error('流程不存在');
    }
    validate(graph);
    def.nodes = graph.nodes.map((n) => ({ ...n }));
    def.edges = graph.edges.map((e) => ({ ...e }));
    if (def.status === 'PUBLISHED') {
      def.status = 'DRAFT';
    }
    touch(def);
  },

  /** 发布:DRAFT → PUBLISHED,生成新版本 */
  publish(id: number): void {
    const def = find(id);
    if (!def) {
      throw new Error('流程不存在');
    }
    if (def.status === 'PUBLISHED') {
      throw new Error('流程已是发布状态');
    }
    validate(def);
    def.status = 'PUBLISHED';
    def.version += 1;
    touch(def);
  },

  /** 停用:PUBLISHED → DISABLED(不再被新工单引用) */
  disable(id: number): void {
    const def = find(id);
    if (!def) {
      throw new Error('流程不存在');
    }
    if (def.status !== 'PUBLISHED') {
      throw new Error('仅已发布流程可停用');
    }
    def.status = 'DISABLED';
    touch(def);
  },

  /** 启用:DISABLED → DRAFT(重新编辑后发布) */
  enable(id: number): void {
    const def = find(id);
    if (!def) {
      throw new Error('流程不存在');
    }
    if (def.status !== 'DISABLED') {
      throw new Error('仅已停用流程可启用');
    }
    def.status = 'DRAFT';
    touch(def);
  },

  /** 删除:仅非发布状态可删 */
  remove(id: number): void {
    const def = find(id);
    if (!def) {
      throw new Error('流程不存在');
    }
    if (def.status === 'PUBLISHED') {
      throw new Error('已发布流程不可删除,请先停用');
    }
    processDefs.splice(processDefs.indexOf(def), 1);
  }
};

