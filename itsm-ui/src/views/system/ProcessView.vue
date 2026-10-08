<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { VueFlow, Handle, Position, useVueFlow, MarkerType } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import type { Edge, Node, Connection, NodeMouseEvent, EdgeMouseEvent } from '@vue-flow/core';
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import {
  processStore, NODE_TYPE_LABEL, NODE_TYPE_TAG, ASSIGNEE_TYPE_LABEL,
  PROCESS_STATUS_LABEL, PROCESS_STATUS_TAG, PROCESS_ROLE_OPTIONS,
  GATEWAY_TYPE_SHORT,
  type ProcessDef, type ProcessNode, type ProcessNodeType, type ProcessEdge,
  type AssigneeType, type GatewayType
} from '../../api/process';
import { TYPE_LABEL, HANDLE_GROUPS } from '../../api/ticket';
import type { TicketType } from '../../api/ticket';

// ===== 列表 =====
const rows = ref<ProcessDef[]>([]);

function load(): void {
  rows.value = processStore.list();
}
onMounted(load);

function fmtTime(v: string): string {
  return new Date(v).toLocaleString('zh-CN', { hour12: false });
}

/** 流程中包含的网关类型(去重) */
function gatewayTypes(def: ProcessDef): GatewayType[] {
  const set = new Set<GatewayType>();
  def.nodes.forEach((n) => {
    if (n.type === 'GATEWAY' && n.gatewayType) {
      set.add(n.gatewayType);
    }
  });
  return [...set];
}

// ===== 新增 / 编辑基本信息 =====
const baseDialog = reactive({ visible: false, isEdit: false, editingId: 0 });
const baseForm = reactive<{ key: string; name: string; ticketType: TicketType; remark: string }>({
  key: '', name: '', ticketType: 'INCIDENT', remark: ''
});
const baseRef = ref();

const baseRules = {
  key: [{ required: true, message: '请输入流程标识', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{2,31}$/, message: '小写字母开头,3-32 位小写字母/数字/下划线', trigger: 'blur' }],
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  ticketType: [{ required: true, message: '请选择绑定工单类型', trigger: 'change' }]
};

function openCreate(): void {
  baseDialog.isEdit = false;
  baseDialog.editingId = 0;
  Object.assign(baseForm, { key: '', name: '', ticketType: 'INCIDENT', remark: '' });
  baseDialog.visible = true;
}
function openEditBase(row: ProcessDef): void {
  baseDialog.isEdit = true;
  baseDialog.editingId = row.id;
  Object.assign(baseForm, { key: row.key, name: row.name, ticketType: row.ticketType, remark: row.remark ?? '' });
  baseDialog.visible = true;
}
function submitBase(): void {
  baseRef.value?.validate((valid: boolean) => {
    if (!valid) {
      return;
    }
    try {
      if (baseDialog.isEdit) {
        processStore.updateInfo(baseDialog.editingId, {
          name: baseForm.name, ticketType: baseForm.ticketType, remark: baseForm.remark.trim() || null
        });
        ElMessage.success('已保存');
      } else {
        const def = processStore.create({
          key: baseForm.key, name: baseForm.name, ticketType: baseForm.ticketType, remark: baseForm.remark.trim() || null
        });
        ElMessage.success(`已创建草稿「${def.name}」,可进入设计器编排节点`);
      }
      baseDialog.visible = false;
      load();
    } catch (e) {
      ElMessage.error((e as Error).message);
    }
  });
}

// ===== 设计器(拖拽画布,Vue Flow) =====

/** 画布节点业务数据 */
interface VfNodeData {
  kind: ProcessNodeType;
  name: string;
  gatewayType: GatewayType | null;
  assigneeType: AssigneeType | null;
  assigneeValue: string | null;
  slaHours: number | null;
}

/** 画布连线业务数据 */
interface VfEdgeData {
  condition: string | null;
}

const designer = reactive<{ visible: boolean; def: ProcessDef | null }>({ visible: false, def: null });
// 元素类型用 any 规避 vue-tsc 对 @vue-flow/core Node/Edge 泛型的深实例化(TS2589);
// 业务数据仍通过 VfNodeData / VfEdgeData 断言访问,不影响运行时
const vfNodes = ref<any[]>([]);
const vfEdges = ref<any[]>([]);
const { screenToFlowCoordinate, fitView } = useVueFlow();

/** 左侧节点面板(拖入画布) */
const PALETTE: Array<{ kind: ProcessNodeType; gatewayType: GatewayType | null; label: string; tip: string }> = [
  { kind: 'TASK', gatewayType: null, label: '处理任务', tip: '人工处理节点' },
  { kind: 'APPROVAL', gatewayType: null, label: '审批任务', tip: '审批通过/驳回' },
  { kind: 'GATEWAY', gatewayType: 'EXCLUSIVE', label: '排他网关', tip: '按条件只走一条分支' },
  { kind: 'GATEWAY', gatewayType: 'PARALLEL', label: '并行网关', tip: '多分支同时推进' }
];

let newSeq = 0;

function kindCount(kind: ProcessNodeType): number {
  return vfNodes.value.filter((n: any) => (n.data as unknown as VfNodeData).kind === kind).length + 1;
}

function defaultName(item: { kind: ProcessNodeType; gatewayType: GatewayType | null }): string {
  if (item.kind === 'GATEWAY') {
    return item.gatewayType === 'EXCLUSIVE' ? `条件分支${kindCount('GATEWAY')}` : `并行分支${kindCount('GATEWAY')}`;
  }
  return `${NODE_TYPE_LABEL[item.kind]}${kindCount(item.kind)}`;
}

function onPaletteDragStart(event: DragEvent, item: (typeof PALETTE)[number]): void {
  event.dataTransfer?.setData('application/itsm-node', JSON.stringify(item));
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy';
  }
}

function onCanvasDrop(event: DragEvent): void {
  const raw = event.dataTransfer?.getData('application/itsm-node');
  if (!raw) {
    return;
  }
  const item = JSON.parse(raw) as (typeof PALETTE)[number];
  const pos = screenToFlowCoordinate({ x: event.clientX, y: event.clientY });
  vfNodes.value.push({
    id: `node_new_${++newSeq}_${Date.now()}`,
    type: 'process',
    position: { x: pos.x - 70, y: pos.y - 20 },
    data: {
      kind: item.kind, name: defaultName(item), gatewayType: item.gatewayType,
      assigneeType: item.kind === 'TASK' || item.kind === 'APPROVAL' ? 'GROUP' : null,
      assigneeValue: item.kind === 'TASK' || item.kind === 'APPROVAL' ? '桌面支持组' : null,
      slaHours: null
    }
  });
}

function onConnect(connection: Connection): void {
  const { source, target } = connection;
  if (!source || !target || source === target) {
    return;
  }
  const srcData = vfNodes.value.find((n) => n.id === source)?.data as VfNodeData | undefined;
  const tgtData = vfNodes.value.find((n) => n.id === target)?.data as VfNodeData | undefined;
  if (srcData?.kind === 'END') {
    ElMessage.warning('「结束」节点不能有出边');
    return;
  }
  if (tgtData?.kind === 'START') {
    ElMessage.warning('「开始」节点不能有入边');
    return;
  }
  if (vfEdges.value.some((e) => e.source === source && e.target === target)) {
    ElMessage.warning('两个节点间已存在连线');
    return;
  }
  vfEdges.value.push({
    id: `edge_new_${++newSeq}_${Date.now()}`, source, target,
    label: undefined, animated: true, markerEnd: MarkerType.ArrowClosed, data: { condition: null }
  });
}

// ---- 选中与属性面板 ----
const sel = reactive<{ type: 'node' | 'edge' | null; id: string | null }>({ type: null, id: null });

const selectedNode = computed<Node | undefined>(() =>
  sel.type === 'node' ? vfNodes.value.find((n) => n.id === sel.id) : undefined);
const selectedEdge = computed<Edge | undefined>(() =>
  sel.type === 'edge' ? vfEdges.value.find((e) => e.id === sel.id) : undefined);

const nodeForm = reactive({
  name: '', kind: 'TASK' as ProcessNodeType, gatewayType: 'EXCLUSIVE' as GatewayType,
  assigneeType: 'GROUP' as AssigneeType, assigneeValue: '', slaHours: null as number | null
});
const edgeForm = reactive({ label: '', condition: '' });

function syncFromSelection(): void {
  const n = selectedNode.value;
  if (n) {
    const d = n.data as VfNodeData;
    Object.assign(nodeForm, {
      name: d.name, kind: d.kind, gatewayType: d.gatewayType ?? 'EXCLUSIVE',
      assigneeType: d.assigneeType ?? 'GROUP', assigneeValue: d.assigneeValue ?? '',
      slaHours: d.slaHours
    });
    return;
  }
  const e = selectedEdge.value;
  if (e) {
    edgeForm.label = typeof e.label === 'string' ? e.label : '';
    edgeForm.condition = ((e.data as VfEdgeData | undefined)?.condition) ?? '';
  }
}

/** 属性编辑回写到画布元素 */
watch([nodeForm, edgeForm], () => {
  const n = selectedNode.value;
  if (n) {
    const d = n.data as VfNodeData;
    d.name = nodeForm.name;
    d.gatewayType = nodeForm.gatewayType;
    d.assigneeType = nodeForm.assigneeType;
    d.assigneeValue = nodeForm.assigneeType === 'INITIATOR' ? '发起人本人' : nodeForm.assigneeValue;
    d.slaHours = nodeForm.slaHours;
  }
  const e = selectedEdge.value;
  if (e) {
    e.label = edgeForm.label.trim() || undefined;
    if (e.data) {
      (e.data as VfEdgeData).condition = edgeForm.condition.trim() || null;
    }
  }
}, { deep: true });

function onNodeClick(e: NodeMouseEvent): void {
  sel.type = 'node';
  sel.id = e.node.id;
  syncFromSelection();
}

function onEdgeClick(e: EdgeMouseEvent): void {
  sel.type = 'edge';
  sel.id = e.edge.id;
  syncFromSelection();
}

function clearSelection(): void {
  sel.type = null;
  sel.id = null;
}

function deleteSelected(): void {
  if (sel.type === 'node') {
    const d = selectedNode.value?.data as VfNodeData | undefined;
    if (d?.kind === 'START' || d?.kind === 'END') {
      ElMessage.warning('开始/结束节点不可删除');
      return;
    }
    vfNodes.value = vfNodes.value.filter((n) => n.id !== sel.id);
    vfEdges.value = vfEdges.value.filter((e) => e.source !== sel.id && e.target !== sel.id);
  } else if (sel.type === 'edge') {
    vfEdges.value = vfEdges.value.filter((e) => e.id !== sel.id);
  }
  clearSelection();
}

const assigneeValueInput = computed(() => {
  if (nodeForm.assigneeType === 'INITIATOR') {
    return 'none';
  }
  return nodeForm.assigneeType === 'USER' ? 'input'
    : nodeForm.assigneeType === 'ROLE' ? 'role' : 'group';
});

const isFixedNode = computed(() =>
  selectedNode.value && ((selectedNode.value.data as VfNodeData).kind === 'START'
    || (selectedNode.value.data as VfNodeData).kind === 'END'));

// ---- 打开 / 保存 ----
function toVfNode(n: ProcessNode): Node {
  return {
    id: n.id, type: 'process', position: { x: n.x, y: n.y },
    data: {
      kind: n.type, name: n.name, gatewayType: n.gatewayType,
      assigneeType: n.assigneeType, assigneeValue: n.assigneeValue, slaHours: n.slaHours
    }
  };
}

function toVfEdge(e: ProcessEdge): Edge {
  return {
    id: e.id, source: e.source, target: e.target,
    label: e.label ?? undefined,
    animated: true,
    class: e.condition ? 'cond-edge' : undefined,
    markerEnd: e.condition
      ? { type: MarkerType.ArrowClosed, color: '#f56c6c' }
      : MarkerType.ArrowClosed,
    data: { condition: e.condition }
  };
}

function openDesigner(row: ProcessDef): void {
  designer.def = row;
  vfNodes.value = row.nodes.map(toVfNode);
  vfEdges.value = row.edges.map(toVfEdge);
  clearSelection();
  designer.visible = true;
  nextTick(() => setTimeout(() => fitView({ padding: 0.15, maxZoom: 1.1, duration: 200 }), 200));
}

function saveDesigner(): void {
  if (!designer.def) {
    return;
  }
  const graph = {
    nodes: vfNodes.value.map((n) => {
      const d = n.data as VfNodeData;
      return {
        id: n.id, name: (d.name || '').trim() || NODE_TYPE_LABEL[d.kind], type: d.kind,
        gatewayType: d.kind === 'GATEWAY' ? d.gatewayType : null,
        assigneeType: d.kind === 'TASK' || d.kind === 'APPROVAL' ? d.assigneeType : null,
        assigneeValue: d.kind === 'TASK' || d.kind === 'APPROVAL' ? d.assigneeValue : null,
        slaHours: d.slaHours ?? null,
        x: Math.round(n.position.x), y: Math.round(n.position.y)
      };
    }),
    edges: vfEdges.value.map((e) => ({
      id: e.id, source: e.source, target: e.target,
      label: (typeof e.label === 'string' && e.label.trim()) || null,
      condition: ((e.data as VfEdgeData | undefined)?.condition || '').trim() || null
    }))
  };
  try {
    processStore.updateNodes(designer.def.id, graph);
    designer.visible = false;
    load();
    ElMessage.success('编排已保存(已发布流程将转回草稿,需重新发布)');
  } catch (e) {
    ElMessage.error((e as Error).message);
  }
}

// ===== 画布节点展示 =====
function nodeBadge(data: VfNodeData): string {
  if (data.kind === 'START') {
    return '▶';
  }
  if (data.kind === 'END') {
    return '■';
  }
  if (data.kind === 'GATEWAY') {
    return data.gatewayType === 'EXCLUSIVE' ? '◇' : '◆';
  }
  return data.kind === 'APPROVAL' ? '✓' : '⚙';
}

function canvasMeta(data: VfNodeData): string {
  if (data.kind === 'TASK' || data.kind === 'APPROVAL') {
    const a = data.assigneeType ? `${ASSIGNEE_TYPE_LABEL[data.assigneeType]}:${data.assigneeValue ?? '-'}` : '未设置';
    return `处理人:${a}${data.slaHours ? ` · SLA ${data.slaHours}h` : ''}`;
  }
  if (data.kind === 'GATEWAY') {
    return data.gatewayType === 'EXCLUSIVE' ? '按条件走一条分支' : '多分支同时推进';
  }
  return '';
}

// ===== 发布 / 停用 / 启用 / 删除 =====
function publish(row: ProcessDef): void {
  ElMessageBox.confirm(
    `发布「${row.name}」?发布后生成 v${row.version + 1} 版本并可供工单流转引用。`,
    '发布确认', { type: 'info' })
    .then(() => {
      try {
        processStore.publish(row.id);
        ElMessage.success(`已发布 v${row.version + 1}`);
        load();
      } catch (e) {
        ElMessage.error((e as Error).message);
      }
    })
    .catch(() => undefined);
}

function disable(row: ProcessDef): void {
  ElMessageBox.confirm(`停用「${row.name}」?停用后新工单不再引用该流程。`, '停用确认', { type: 'warning' })
    .then(() => {
      try {
        processStore.disable(row.id);
        ElMessage.success('已停用');
        load();
      } catch (e) {
        ElMessage.error((e as Error).message);
      }
    })
    .catch(() => undefined);
}

function enable(row: ProcessDef): void {
  try {
    processStore.enable(row.id);
    ElMessage.success('已启用为草稿,可编辑后重新发布');
    load();
  } catch (e) {
    ElMessage.error((e as Error).message);
  }
}

function remove(row: ProcessDef): void {
  ElMessageBox.confirm(`确认删除流程「${row.name}」?`, '删除确认', { type: 'warning' })
    .then(() => {
      try {
        processStore.remove(row.id);
        ElMessage.success('已删除');
        load();
      } catch (e) {
        ElMessage.error((e as Error).message);
      }
    })
    .catch(() => undefined);
}
</script>

<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="hint">拖拽式流程编排:支持串行 / 并行 / 条件分支;对接 Flowable 后保存编排即生成 BPMN 并部署</span>
      <div class="spacer" />
      <el-button type="primary" @click="openCreate">新增流程</el-button>
    </div>

    <el-table :data="rows" stripe>
      <el-table-column prop="key" label="流程标识" min-width="140" />
      <el-table-column prop="name" label="流程名称" min-width="140" />
      <el-table-column label="版本" width="70" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">v{{ row.version }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="绑定类型" width="95">
        <template #default="{ row }">{{ TYPE_LABEL[row.ticketType as keyof typeof TYPE_LABEL] }}</template>
      </el-table-column>
      <el-table-column label="流程结构" min-width="220">
        <template #default="{ row }">
          <span class="struct-stat">{{ row.nodes.length }} 节点</span>
          <el-divider direction="vertical" />
          <span class="struct-stat">{{ row.edges.length }} 连线</span>
          <template v-if="gatewayTypes(row).length">
            <el-divider direction="vertical" />
            <el-tag
              v-for="gt in gatewayTypes(row)" :key="gt" size="small" type="danger"
              effect="plain" class="gw-tag"
            >{{ GATEWAY_TYPE_SHORT[gt] }}网关</el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="85" align="center">
        <template #default="{ row }">
          <el-tag :type="PROCESS_STATUS_TAG[row.status as keyof typeof PROCESS_STATUS_TAG]" size="small">
            {{ PROCESS_STATUS_LABEL[row.status as keyof typeof PROCESS_STATUS_LABEL] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">{{ fmtTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDesigner(row)">设计</el-button>
          <el-button link type="primary" @click="openEditBase(row)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="publish(row)">发布</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="disable(row)">停用</el-button>
          <el-button v-if="row.status === 'DISABLED'" link type="success" @click="enable(row)">启用</el-button>
          <el-button link type="danger" :disabled="row.status === 'PUBLISHED'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="muted tip">原型:可视化流程图(开始/任务/审批/排他与并行网关/结束),条件分支支持表达式配置</div>
  </el-card>

  <!-- 流程设计器(拖拽画布) -->
  <el-drawer v-model="designer.visible" size="94%" :title="`流程设计 - ${designer.def?.name ?? ''}`">
    <div v-if="designer.def" class="designer-head">
      <el-tag size="small" type="info">{{ designer.def.key }}</el-tag>
      <el-tag size="small">v{{ designer.def.version }}</el-tag>
      <el-tag :type="PROCESS_STATUS_TAG[designer.def.status]" size="small">
        {{ PROCESS_STATUS_LABEL[designer.def.status] }}
      </el-tag>
      <span class="muted">修改已发布流程保存后自动转回草稿,需重新发布</span>
    </div>

    <div class="designer-body">
      <!-- 左侧节点面板 -->
      <div class="palette">
        <div class="palette-title">节点(拖入画布)</div>
        <div
          v-for="item in PALETTE" :key="item.label" class="palette-item"
          :class="item.kind.toLowerCase() + (item.gatewayType ? '-' + item.gatewayType.toLowerCase() : '')"
          draggable="true" @dragstart="onPaletteDragStart($event, item)"
        >
          <span class="pi-badge">{{ item.gatewayType === 'EXCLUSIVE' ? '◇' : item.gatewayType === 'PARALLEL' ? '◆' : item.kind === 'APPROVAL' ? '✓' : '⚙' }}</span>
          <span class="pi-label">{{ item.label }}</span>
        </div>
        <div class="palette-help">
          <p>· 拖拽节点到画布添加</p>
          <p>· 拖动节点底部圆点连线</p>
          <p>· 点击节点/连线编辑属性</p>
          <p>· 串行:顺序连线</p>
          <p>· 并行:并行网关拉出多条分支</p>
          <p>· 条件:排他网关出边配表达式</p>
        </div>
      </div>

      <!-- 画布 -->
      <div class="canvas-wrap" @drop.prevent="onCanvasDrop" @dragover.prevent>
        <VueFlow
          v-model:nodes="vfNodes"
          v-model:edges="vfEdges"
          :snap-to-grid="true"
          :snap-grid="[10, 10]"
          :min-zoom="0.2"
          :max-zoom="1.6"
          :connection-radius="30"
          :delete-key-code="null"
          @connect="onConnect"
          @node-click="onNodeClick"
          @edge-click="onEdgeClick"
          @pane-click="clearSelection"
        >
          <Background :gap="18" pattern-color="#dbe4f3" />
          <template #node-process="{ data, selected }">
            <div
              class="pnode"
              :class="[
                data.kind.toLowerCase(),
                data.kind === 'GATEWAY' ? (data.gatewayType === 'PARALLEL' ? 'par' : 'exc') : '',
                selected ? 'sel' : ''
              ]"
            >
              <Handle v-if="data.kind !== 'START'" type="target" :position="Position.Top" />
              <Handle v-if="data.kind !== 'END'" type="source" :position="Position.Bottom" />
              <div class="pnode-head">
                <span class="pnode-badge">{{ nodeBadge(data) }}</span>
                <span class="pnode-type">{{ NODE_TYPE_LABEL[data.kind as ProcessNodeType] }}</span>
              </div>
              <div class="pnode-name">{{ data.name }}</div>
              <div v-if="canvasMeta(data)" class="pnode-meta">{{ canvasMeta(data) }}</div>
            </div>
          </template>
        </VueFlow>
      </div>

      <!-- 右侧属性面板 -->
      <div class="props">
        <template v-if="selectedNode">
          <div class="props-title">节点属性</div>
          <el-form label-position="top" size="small" @submit.prevent>
            <el-form-item label="节点名称">
              <el-input v-model="nodeForm.name" maxlength="30" :disabled="!!isFixedNode" />
            </el-form-item>
            <el-form-item label="类型">
              <el-tag :type="NODE_TYPE_TAG[nodeForm.kind]" size="small">{{ NODE_TYPE_LABEL[nodeForm.kind] }}</el-tag>
              <el-tag v-if="nodeForm.kind === 'GATEWAY'" size="small" type="danger" style="margin-left: 6px">
                {{ GATEWAY_TYPE_SHORT[nodeForm.gatewayType] }}网关
              </el-tag>
            </el-form-item>
            <template v-if="nodeForm.kind === 'GATEWAY'">
              <el-form-item label="网关类型">
                <el-radio-group v-model="nodeForm.gatewayType">
                  <el-radio-button value="EXCLUSIVE">排他</el-radio-button>
                  <el-radio-button value="PARALLEL">并行</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-alert
                type="warning" :closable="false"
                title="排他:出边配条件表达式,只走一条;并行:所有出边同时激活,各分支汇入同一节点"
              />
            </template>
            <template v-else-if="nodeForm.kind === 'TASK' || nodeForm.kind === 'APPROVAL'">
              <el-form-item label="处理人类型">
                <el-select v-model="nodeForm.assigneeType" style="width: 100%">
                  <el-option label="发起人" value="INITIATOR" />
                  <el-option label="指定用户" value="USER" />
                  <el-option label="指定角色" value="ROLE" />
                  <el-option label="处理组" value="GROUP" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="assigneeValueInput === 'input'" label="处理人">
                <el-input v-model="nodeForm.assigneeValue" placeholder="用户名,如 admin" />
              </el-form-item>
              <el-form-item v-else-if="assigneeValueInput === 'role'" label="处理角色">
                <el-select v-model="nodeForm.assigneeValue" style="width: 100%">
                  <el-option v-for="r in PROCESS_ROLE_OPTIONS" :key="r" :label="r" :value="r" />
                </el-select>
              </el-form-item>
              <el-form-item v-else-if="assigneeValueInput === 'group'" label="处理组">
                <el-select v-model="nodeForm.assigneeValue" style="width: 100%">
                  <el-option v-for="g in HANDLE_GROUPS" :key="g" :label="g" :value="g" />
                </el-select>
              </el-form-item>
              <el-form-item v-else label="处理人">
                <span class="muted">自动流转至发起人本人</span>
              </el-form-item>
              <el-form-item label="节点 SLA(小时)">
                <el-input-number v-model="nodeForm.slaHours" :min="0.5" :max="720" :step="0.5" style="width: 100%" />
              </el-form-item>
            </template>
            <el-form-item v-else label="说明">
              <span class="muted">{{ nodeForm.kind === 'START' ? '流程入口,每个流程仅一个' : '流程出口,所有分支收敛于此' }}</span>
            </el-form-item>
            <el-button v-if="!isFixedNode" type="danger" plain size="small" style="width: 100%" @click="deleteSelected">
              删除节点
            </el-button>
          </el-form>
        </template>

        <template v-else-if="selectedEdge">
          <div class="props-title">连线属性</div>
          <el-form label-position="top" size="small" @submit.prevent>
            <el-form-item label="连线标签">
              <el-input v-model="edgeForm.label" maxlength="20" placeholder="如:同意 / 驳回" />
            </el-form-item>
            <el-form-item label="条件表达式">
              <el-input v-model="edgeForm.condition" placeholder="${urgency} == 'U3'" />
              <div class="muted">排他网关出边生效,如 ${priority} == 'P1';留空视为默认分支</div>
            </el-form-item>
            <el-button type="danger" plain size="small" style="width: 100%" @click="deleteSelected">删除连线</el-button>
          </el-form>
        </template>

        <template v-else>
          <div class="props-title">操作指引</div>
          <ul class="props-help">
            <li>从左侧拖拽节点到画布</li>
            <li>拖动节点底部圆点进行连线</li>
            <li>点击节点 / 连线编辑属性</li>
            <li>滚轮缩放,拖拽空白处平移</li>
          </ul>
        </template>
      </div>
    </div>

    <template #footer>
      <el-button @click="designer.visible = false">取消</el-button>
      <el-button type="primary" @click="saveDesigner">保存编排</el-button>
    </template>
  </el-drawer>

  <!-- 新增 / 编辑流程基本信息 -->
  <el-dialog v-model="baseDialog.visible" :title="baseDialog.isEdit ? '编辑流程' : '新增流程'" width="520px">
    <el-form ref="baseRef" :model="baseForm" :rules="baseRules" label-width="90px">
      <el-form-item label="流程标识" prop="key">
        <el-input v-model="baseForm.key" :disabled="baseDialog.isEdit" placeholder="如 ticket_incident" />
      </el-form-item>
      <el-form-item label="流程名称" prop="name">
        <el-input v-model="baseForm.name" maxlength="50" />
      </el-form-item>
      <el-form-item label="绑定类型" prop="ticketType">
        <el-select v-model="baseForm.ticketType" style="width: 100%">
          <el-option v-for="(label, key) in TYPE_LABEL" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="baseForm.remark" type="textarea" :rows="2" maxlength="200" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="baseDialog.visible = false">取消</el-button>
      <el-button type="primary" @click="submitBase">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.spacer {
  flex: 1;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.tip {
  margin-top: 10px;
}

.struct-stat {
  font-size: 13px;
  color: #606266;
}

.gw-tag {
  margin-left: 4px;
}

.designer-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.designer-body {
  display: flex;
  gap: 10px;
  height: calc(100% - 40px);
  min-height: 420px;
}

/* ---- 左侧节点面板 ---- */
.palette {
  width: 150px;
  flex-shrink: 0;
  border: 1px solid #e4e9f2;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px 8px;
  overflow: auto;
}

.palette-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  cursor: grab;
  border-left-width: 3px;
  transition: box-shadow 0.15s, transform 0.15s;
}

.palette-item:hover {
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.18);
  transform: translateY(-1px);
}

.palette-item:active {
  cursor: grabbing;
}

.palette-item.task {
  border-left-color: #1677ff;
}

.palette-item.approval {
  border-left-color: #e6a23c;
}

.palette-item.gateway-exc {
  border-left-color: #f56c6c;
}

.palette-item.gateway-par {
  border-left-color: #722ed1;
}

.pi-badge {
  font-size: 14px;
}

.palette-item.task .pi-badge {
  color: #1677ff;
}

.palette-item.approval .pi-badge {
  color: #e6a23c;
}

.palette-item.gateway-exc .pi-badge {
  color: #f56c6c;
}

.palette-item.gateway-par .pi-badge {
  color: #722ed1;
}

.palette-help {
  margin-top: 10px;
  font-size: 11px;
  color: #909399;
  line-height: 1.7;
}

.palette-help p {
  margin: 0;
}

/* ---- 画布 ---- */
.canvas-wrap {
  flex: 1;
  border: 1px solid #e4e9f2;
  border-radius: 8px;
  background: #f5f8fd;
  overflow: hidden;
  min-height: 420px;
}

.canvas-wrap :deep(.vue-flow__edge.cond-edge .vue-flow__edge-path) {
  stroke: #f56c6c;
  stroke-dasharray: 6 3;
}

/* ---- 自定义节点 ---- */
.pnode {
  width: 150px;
  background: #fff;
  border: 1.5px solid #1677ff;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  box-shadow: 0 1px 4px rgba(15, 39, 84, 0.08);
}

.pnode.start {
  border-color: #67c23a;
  border-radius: 18px;
  text-align: center;
}

.pnode.end {
  border-color: #909399;
  border-radius: 18px;
  text-align: center;
}

.pnode.task {
  border-color: #1677ff;
}

.pnode.approval {
  border-color: #e6a23c;
  background: #fffdf5;
}

.pnode.gateway.exc {
  border-color: #f56c6c;
  background: #fffafa;
}

.pnode.gateway.par {
  border-color: #722ed1;
  background: #fcfaff;
}

.pnode.sel {
  box-shadow: 0 0 0 2.5px rgba(22, 119, 255, 0.3);
}

.pnode-head {
  display: flex;
  align-items: center;
  gap: 5px;
}

.pnode-badge {
  font-size: 13px;
}

.pnode.task .pnode-badge {
  color: #1677ff;
}

.pnode.approval .pnode-badge {
  color: #e6a23c;
}

.pnode.gateway.exc .pnode-badge {
  color: #f56c6c;
}

.pnode.gateway.par .pnode-badge {
  color: #722ed1;
}

.pnode.start .pnode-badge {
  color: #67c23a;
}

.pnode.end .pnode-badge {
  color: #909399;
}

.pnode-type {
  font-size: 11px;
  color: #909399;
}

.pnode-name {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pnode-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- 右侧属性面板 ---- */
.props {
  width: 250px;
  flex-shrink: 0;
  border: 1px solid #e4e9f2;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px 12px;
  overflow: auto;
}

.props-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e9f2;
}

.props-help {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  color: #909399;
  line-height: 2;
}
</style>

