<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  ticketStore, suggestPriority, TYPE_LABEL, IMPACT_LABEL, URGENCY_LABEL,
  PRIORITY_LABEL, HANDLE_GROUPS, CATALOG_TREE, CURRENT_USER
} from '../../api/ticket';
import type { TicketType, Impact, Urgency, Priority } from '../../api/ticket';

const route = useRoute();
const router = useRouter();

/** 草稿编辑模式(从「我的草稿」进入) */
const editingId = ref<string | null>(null);

const step = ref(0);

// ===== 步骤 1:类型 + 服务目录条目 =====
const type = ref<TicketType>('INCIDENT');
const catalogPath = ref<string[]>([]);

const catalogLabel = computed(() => {
  if (catalogPath.value.length === 0) return '';
  const [parentValue, childValue] = catalogPath.value;
  const parent = CATALOG_TREE.find((g) => g.value === parentValue);
  const child = parent?.children.find((c) => c.value === childValue);
  return parent && child ? `${parent.label} / ${child.label}` : '';
});

// ===== 步骤 2:工单信息 =====
const form = reactive({
  title: '',
  description: '',
  assigneeGroup: '服务台',
  impact: 'I1' as Impact,
  urgency: 'U2' as Urgency,
  priority: 'P3' as Priority,
  priorityTouched: false
});

/** 优先级自动建议(影响度 × 紧急度),可人工修正 */
function onMatrixChange(): void {
  if (!form.priorityTouched) {
    form.priority = suggestPriority(form.impact, form.urgency);
  }
}

const step1Valid = computed(() =>
  type.value === 'REQUEST' ? catalogLabel.value !== '' : true);

/** 根据目录条目展示名反查级联路径(草稿回填用) */
function findCatalogPath(label: string): string[] {
  for (const g of CATALOG_TREE) {
    for (const c of g.children) {
      if (`${g.label} / ${c.label}` === label) {
        return [g.value, c.value];
      }
    }
  }
  return [];
}

const step2Valid = computed(() =>
  form.title.trim().length > 0 && form.description.trim().length > 0);

function next(): void {
  if (step.value === 0 && !step1Valid.value) {
    ElMessage.warning('服务请求必须选择服务目录条目');
    return;
  }
  if (step.value === 1 && !step2Valid.value) {
    ElMessage.warning('请填写标题与问题描述');
    return;
  }
  step.value += 1;
}

function prev(): void {
  step.value = Math.max(step.value - 1, 0);
}

// ===== 步骤 3:提交 / 保存草稿 =====
function buildPayload() {
  return {
    type: type.value,
    catalogItem: catalogLabel.value || undefined,
    title: form.title.trim(),
    description: form.description.trim(),
    impact: form.impact,
    urgency: form.urgency,
    priority: form.priority,
    assigneeGroup: form.assigneeGroup
  };
}

function submit(): void {
  if (editingId.value) {
    ticketStore.saveDraft(editingId.value, buildPayload());
    const t = ticketStore.act(editingId.value, 'submit', '草稿正式提交');
    ElMessage.success(`草稿已正式提交,工单 ${t.no} 进入受理流程,SLA 倒计时已启动`);
    router.push(`/tickets/${t.id}`);
    return;
  }
  const t = ticketStore.create(buildPayload());
  ElMessage.success(`工单 ${t.no} 已提交,SLA 倒计时已启动`);
  router.push(`/tickets/${t.id}`);
}

function saveAsDraft(): void {
  const payload = { ...buildPayload(), draft: true };
  if (editingId.value) {
    ticketStore.saveDraft(editingId.value, payload);
    ElMessage.success('草稿已保存');
  } else {
    const t = ticketStore.create(payload);
    ElMessage.success(`已保存为草稿 ${t.no}`);
  }
  router.push('/my-tickets/drafts');
}

// 草稿回填:仅草稿本人可编辑
onMounted(() => {
  const id = route.query.id;
  if (typeof id !== 'string' || !id) {
    return;
  }
  const t = ticketStore.get(id);
  if (!t || t.status !== 'DRAFT' || t.reporter !== CURRENT_USER) {
    ElMessage.warning('草稿不存在或不可编辑');
    return;
  }
  editingId.value = t.id;
  type.value = t.type;
  Object.assign(form, {
    title: t.title,
    description: t.description,
    assigneeGroup: t.assigneeGroup,
    impact: t.impact,
    urgency: t.urgency,
    priority: t.priority,
    priorityTouched: true
  });
  const createAction = t.actions.find((a) => a.type === 'create');
  const m = createAction?.comment.match(/通过服务目录「(.+?)」/);
  if (m) {
    catalogPath.value = findCatalogPath(m[1]);
  }
});
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <span>{{ editingId ? '编辑草稿' : '新建工单' }}</span>
      <span style="color: #98a2b3; font-size: 12px; margin-left: 12px">
        当前用户:{{ CURRENT_USER }}(原型数据,提交仅存内存)
      </span>
    </template>

    <el-steps :active="step" align-center finish-status="success" style="margin-bottom: 28px">
      <el-step title="选择类型" description="事件 / 请求 / 问题 / 变更" />
      <el-step title="填写信息" description="优先级矩阵自动建议" />
      <el-step title="确认提交" description="启动 SLA 计时" />
    </el-steps>

    <!-- 步骤 1:类型 -->
    <div v-if="step === 0" class="step-body">
      <el-row :gutter="16">
        <el-col v-for="(label, key) in TYPE_LABEL" :key="key" :span="6">
          <div
            class="type-card"
            :class="{ active: type === key }"
            @click="type = key"
          >
            <div class="type-name">{{ label }}</div>
            <div class="type-desc">
              {{
                {
                  INCIDENT: '故障报修,尽快恢复服务',
                  REQUEST: '标准服务申请,走预置审批',
                  PROBLEM: '根因定位,防止重复事件',
                  CHANGE: '受控变更,降低人为事故'
                }[key]
              }}
            </div>
          </div>
        </el-col>
      </el-row>

      <el-form v-if="type === 'REQUEST'" label-width="100px" style="margin-top: 24px; max-width: 560px">
        <el-form-item label="服务目录" required>
          <el-cascader
            v-model="catalogPath"
            :options="CATALOG_TREE"
            placeholder="请选择目录条目(绑定表单模板与审批流)"
            style="width: 100%"
          />
        </el-form-item>
        <el-alert
          v-if="catalogLabel"
          type="info"
          :closable="false"
          :title="`条目「${catalogLabel}」已绑定预置表单模板与审批流(原型统一使用简化表单)`"
        />
      </el-form>
    </div>

    <!-- 步骤 2:信息 -->
    <div v-else-if="step === 1" class="step-body">
      <el-form label-width="100px" style="max-width: 640px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="一句话描述问题或需求" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="详细描述" required>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="现象、开始时间、影响范围、已尝试的操作…(原型:附件上传后续接入)"
          />
        </el-form-item>
        <el-form-item label="处理组">
          <el-select v-model="form.assigneeGroup" style="width: 240px">
            <el-option v-for="g in HANDLE_GROUPS" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="影响度">
          <el-radio-group v-model="form.impact" @change="onMatrixChange">
            <el-radio-button v-for="(label, key) in IMPACT_LABEL" :key="key" :value="key">{{ label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="紧急度">
          <el-radio-group v-model="form.urgency" @change="onMatrixChange">
            <el-radio-button v-for="(label, key) in URGENCY_LABEL" :key="key" :value="key">{{ label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width: 160px" @change="form.priorityTouched = true">
            <el-option v-for="(label, key) in PRIORITY_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
          <span class="hint">已按「影响度 × 紧急度」矩阵自动建议,可人工修正</span>
        </el-form-item>
      </el-form>
    </div>

    <!-- 步骤 3:确认 -->
    <div v-else class="step-body">
      <el-descriptions :column="2" border style="max-width: 760px">
        <el-descriptions-item label="工单类型">{{ TYPE_LABEL[type] }}</el-descriptions-item>
        <el-descriptions-item v-if="type === 'REQUEST'" label="服务目录">{{ catalogLabel }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ form.title }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ form.description }}</el-descriptions-item>
        <el-descriptions-item label="影响度">{{ IMPACT_LABEL[form.impact] }}</el-descriptions-item>
        <el-descriptions-item label="紧急度">{{ URGENCY_LABEL[form.urgency] }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ PRIORITY_LABEL[form.priority] }}</el-descriptions-item>
        <el-descriptions-item label="处理组">{{ form.assigneeGroup }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        type="warning"
        :closable="false"
        style="margin-top: 16px; max-width: 760px"
        title="提交后立即启动 SLA 计时(P1:15 分钟响应 / 4 小时解决)"
      />
    </div>

    <!-- 步骤操作 -->
    <div class="step-actions">
      <el-button v-if="step > 0" @click="prev">上一步</el-button>
      <el-button v-if="step < 2" type="primary" @click="next">下一步</el-button>
      <template v-if="step === 2">
        <el-button @click="saveAsDraft">{{ editingId ? '保存修改(仍为草稿)' : '保存为草稿' }}</el-button>
        <el-button type="primary" @click="submit">提交工单</el-button>
      </template>
    </div>
  </el-card>
</template>

<style scoped>
.step-body {
  min-height: 320px;
}

.type-card {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.2s;
  height: 110px;
  box-sizing: border-box;
}

.type-card:hover {
  border-color: #409eff;
}

.type-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.type-name {
  font-weight: 600;
  margin-bottom: 8px;
}

.type-desc {
  color: #98a2b3;
  font-size: 12px;
  line-height: 1.6;
}

.hint {
  color: #98a2b3;
  font-size: 12px;
  margin-left: 12px;
}

.step-actions {
  margin-top: 24px;
  text-align: center;
}
</style>

