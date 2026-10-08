<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  ticketStore, slaRemainingMs, canAct, TYPE_LABEL, TYPE_TAG, STATUS_LABEL, STATUS_TAG,
  PRIORITY_LABEL, PRIORITY_TAG, IMPACT_LABEL, URGENCY_LABEL, ACTION_LABEL, HANDLE_GROUPS
} from '../../api/ticket';
import type { Ticket, TicketActionType } from '../../api/ticket';

const route = useRoute();
const router = useRouter();

const ticket = ref<Ticket | undefined>(ticketStore.get(String(route.params.id)));

if (!ticket.value) {
  ElMessage.error('工单不存在(原型数据可能已刷新重置)');
  router.replace('/tickets');
}

// ===== SLA 进度 =====
const sla = computed(() => {
  if (!ticket.value) return null;
  const total = new Date(ticket.value.slaResolveDeadline).getTime()
    - new Date(ticket.value.createdAt).getTime();
  const elapsed = Date.now() - new Date(ticket.value.createdAt).getTime();
  const remaining = slaRemainingMs(ticket.value);
  const closed = ticket.value.status === 'CLOSED';
  return {
    percent: Math.min(Math.max((elapsed / total) * 100, 0), 100),
    status: remaining < 0 ? 'exception' : (remaining < total * 0.2 ? 'warning' : 'success'),
    text: closed
      ? '已关闭'
      : remaining >= 0
        ? `解决剩余 ${format(remaining)}`
        : `已超时 ${format(-remaining)}`
  };
});

function format(ms: number): string {
  const h = ms / 3600_000;
  return h >= 1 ? `${h.toFixed(1)} 小时` : `${Math.round(ms / 60_000)} 分钟`;
}

function fmtTime(v: string): string {
  return new Date(v).toLocaleString('zh-CN', { hour12: false });
}

// ===== 状态流转动作(与精简版 4.1 状态机一致) =====
const comment = ref('');

interface ActionButton {
  action: Exclude<TicketActionType, 'create'>;
  label: string;
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  plain?: boolean;
}

const BUTTONS: ActionButton[] = [
  { action: 'accept', label: '受理', type: 'primary' },
  { action: 'cancel', label: '无效关闭', type: 'danger', plain: true },
  { action: 'start', label: '开始处理', type: 'primary' },
  { action: 'assign', label: '转派', type: 'info', plain: true },
  { action: 'suspend', label: '挂起', type: 'warning', plain: true },
  { action: 'resume', label: '恢复处理', type: 'primary' },
  { action: 'resolve', label: '提交解决方案', type: 'success' },
  { action: 'reopen', label: '重开', type: 'warning', plain: true },
  { action: 'close', label: '确认关闭', type: 'success' }
];

const visibleButtons = computed(() =>
  ticket.value ? BUTTONS.filter((b) => canAct(ticket.value!.status, b.action)) : []);

const busy = ref(false);

async function doAct(btn: ActionButton): Promise<void> {
  if (!ticket.value) return;
  if (btn.action === 'assign') {
    assignVisible.value = true;
    return;
  }
  if ((btn.action === 'resolve' || btn.action === 'suspend' || btn.action === 'cancel')
    && !comment.value.trim()) {
    ElMessage.warning(btn.action === 'resolve'
      ? '请填写解决方案说明' : `请填写「${btn.label}」原因`);
    return;
  }
  busy.value = true;
  try {
    ticketStore.act(ticket.value.id, btn.action, comment.value.trim());
    ElMessage.success(`已${ACTION_LABEL[btn.action]}`);
    comment.value = '';
    ticket.value = ticketStore.get(ticket.value.id);
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败');
  } finally {
    busy.value = false;
  }
}

// ===== 转派对话框 =====
const assignVisible = ref(false);
const assignTarget = ref('');

function confirmAssign(): void {
  if (!ticket.value || !assignTarget.value) return;
  ticketStore.act(ticket.value.id, 'assign',
    comment.value.trim() || `转派至 ${assignTarget.value}`, assignTarget.value);
  ElMessage.success(`已转派至 ${assignTarget.value}`);
  assignVisible.value = false;
  comment.value = '';
  ticket.value = ticketStore.get(ticket.value.id);
}
</script>

<template>
  <div v-if="ticket">
    <!-- 头部 -->
    <el-card shadow="never" class="head-card">
      <div class="head">
        <div class="head-left">
          <el-button text @click="router.push('/tickets')">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
          <span class="no">{{ ticket.no }}</span>
          <el-tag :type="TYPE_TAG[ticket.type]" effect="plain">{{ TYPE_LABEL[ticket.type] }}</el-tag>
          <el-tag :type="STATUS_TAG[ticket.status]">{{ STATUS_LABEL[ticket.status] }}</el-tag>
          <el-tag :type="PRIORITY_TAG[ticket.priority]" effect="dark">{{ ticket.priority }}</el-tag>
        </div>
      </div>
      <h3 style="margin: 12px 0 0">{{ ticket.title }}</h3>
    </el-card>

    <el-row :gutter="16">
      <!-- 左:描述 + 时间线 + 动作 -->
      <el-col :span="15">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>问题描述</template>
          <p class="desc">{{ ticket.description }}</p>
        </el-card>

        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>处理时间线({{ ticket.actions.length }})</template>
          <el-timeline>
            <el-timeline-item
              v-for="a in [...ticket.actions].reverse()"
              :key="a.id"
              :timestamp="fmtTime(a.at)"
              :type="a.type === 'create' ? 'primary' : a.type === 'resolve' ? 'success' : undefined"
            >
              <b>{{ ACTION_LABEL[a.type] }}</b>
              <span style="color: #98a2b3; margin-left: 8px">{{ a.operator }}</span>
              <div style="color: #475467; font-size: 13px; margin-top: 2px">{{ a.comment }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 动作区 -->
        <el-card shadow="never" v-if="ticket.status !== 'CLOSED'">
          <template #header>处理动作</template>
          <el-input
            v-model="comment"
            type="textarea"
            :rows="3"
            placeholder="填写处理意见 / 解决方案说明(转派可留空)"
          />
          <div style="margin-top: 12px">
            <el-button
              v-for="b in visibleButtons"
              :key="b.action"
              :type="b.type"
              :plain="b.plain"
              :loading="busy"
              @click="doAct(b)"
            >
              {{ b.label }}
            </el-button>
          </div>
        </el-card>
        <el-card shadow="never" v-else>
          <el-result icon="success" title="工单已关闭" sub-title="如问题复现,请重新提交工单并关联本单号" />
        </el-card>
      </el-col>

      <!-- 右:基本信息 + SLA -->
      <el-col :span="9">
        <el-card shadow="never" style="margin-bottom: 16px" v-if="sla">
          <template #header>SLA(解决时限)</template>
          <el-progress
            :percentage="sla.percent"
            :status="sla.status as 'success' | 'warning' | 'exception'"
            :stroke-width="14"
          />
          <div style="text-align: center; margin-top: 10px">
            <el-tag :type="sla.status === 'success' ? 'success' : sla.status === 'warning' ? 'warning' : 'danger'">
              {{ sla.text }}
            </el-tag>
          </div>
          <el-descriptions :column="1" size="small" style="margin-top: 16px">
            <el-descriptions-item label="响应截止">{{ fmtTime(ticket.slaResponseDeadline) }}</el-descriptions-item>
            <el-descriptions-item label="解决截止">{{ fmtTime(ticket.slaResolveDeadline) }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            type="info"
            :closable="false"
            title="挂起期间 SLA 时钟暂停"
            style="margin-top: 8px"
          />
        </el-card>

        <el-card shadow="never">
          <template #header>基本信息</template>
          <el-descriptions :column="1">
            <el-descriptions-item label="提单人">{{ ticket.reporter }}</el-descriptions-item>
            <el-descriptions-item label="处理组">{{ ticket.assigneeGroup }}</el-descriptions-item>
            <el-descriptions-item label="处理人">{{ ticket.assignee ?? '待分派' }}</el-descriptions-item>
            <el-descriptions-item label="影响度">{{ IMPACT_LABEL[ticket.impact] }}</el-descriptions-item>
            <el-descriptions-item label="紧急度">{{ URGENCY_LABEL[ticket.urgency] }}</el-descriptions-item>
            <el-descriptions-item label="优先级">{{ PRIORITY_LABEL[ticket.priority] }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ fmtTime(ticket.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ fmtTime(ticket.updatedAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <!-- 转派对话框 -->
    <el-dialog v-model="assignVisible" title="转派工单" width="420px">
      <el-select v-model="assignTarget" placeholder="选择处理组" style="width: 100%">
        <el-option v-for="g in HANDLE_GROUPS" :key="g" :label="g" :value="g" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!assignTarget" @click="confirmAssign">确认转派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head-card {
  margin-bottom: 16px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.no {
  font-size: 16px;
  font-weight: 600;
}

.desc {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #344054;
  margin: 0;
}
</style>

