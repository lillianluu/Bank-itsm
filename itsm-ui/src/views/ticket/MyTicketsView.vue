<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ticketStore, TYPE_LABEL, TYPE_TAG, STATUS_LABEL, STATUS_TAG,
  PRIORITY_LABEL, PRIORITY_TAG, CURRENT_USER
} from '../../api/ticket';
import type { Ticket, TicketStatus, TicketType, Priority } from '../../api/ticket';

type TabKey = 'drafts' | 'todo' | 'submitted' | 'involved';

const route = useRoute();
const router = useRouter();

/** 四类视图页签:与子路由一一对应 */
const TABS: { key: TabKey; label: string }[] = [
  { key: 'drafts', label: '我的草稿' },
  { key: 'todo', label: '我的待办' },
  { key: 'submitted', label: '我提交的' },
  { key: 'involved', label: '我参与的' }
];

/** 当前页签由路由驱动(/my-tickets/{drafts|todo|submitted|participated}) */
const activeTab = computed<TabKey>({
  get: () => {
    const seg = route.path.replace('/my-tickets/', '');
    return TABS.some((t) => t.key === seg) ? (seg as TabKey) : 'todo';
  },
  set: (v: TabKey) => router.push(`/my-tickets/${v === 'drafts' ? 'drafts' : v}`)
});

// ===== 检索条件(切换页签重置) =====
const filter = reactive<{ keyword: string; type?: TicketType; priority?: Priority; status?: TicketStatus }>({ keyword: '' });

function resetFilter(): void {
  filter.keyword = '';
  filter.type = undefined;
  filter.priority = undefined;
  filter.status = undefined;
}

/** 状态筛选项:草稿页签无意义,其余页签排除 DRAFT */
const statusOptions = computed<Record<string, string>>(() => {
  const entries = Object.entries(STATUS_LABEL).filter(([k]) => k !== 'DRAFT');
  return Object.fromEntries(entries);
});

// ===== 数据(内存仓库非响应式,用 refreshTick 在操作后强制重算) =====
const refreshTick = ref(0);

const mine = computed(() => {
  refreshTick.value;
  return ticketStore.mine();
});

const counts = computed(() => ({
  drafts: mine.value.drafts.length,
  todo: mine.value.todo.length,
  submitted: mine.value.submitted.length,
  involved: mine.value.involved.length
}));

const rows = computed<Ticket[]>(() => mine.value[activeTab.value]
  .filter((t) => (!filter.keyword || t.title.includes(filter.keyword) || t.no.includes(filter.keyword))
    && (!filter.type || t.type === filter.type)
    && (!filter.priority || t.priority === filter.priority)
    && (!filter.status || t.status === filter.status)));

function fmtTime(v: string): string {
  return new Date(v).toLocaleString('zh-CN', { hour12: false });
}

// ===== 草稿操作 =====
function submitDraft(row: Ticket): void {
  ElMessageBox.confirm(
    `确认正式提交草稿「${row.title}」?提交后进入受理流程并启动 SLA 计时。`,
    '提交确认', { type: 'info' })
    .then(() => {
      try {
        ticketStore.act(row.id, 'submit', '草稿正式提交');
        refreshTick.value++;
        ElMessage.success('已提交,进入受理流程');
      } catch (e) {
        ElMessage.error((e as Error).message);
      }
    })
    .catch(() => undefined);
}

function deleteDraft(row: Ticket): void {
  ElMessageBox.confirm(`确认删除草稿「${row.title}」?删除后不可恢复。`, '删除确认', { type: 'warning' })
    .then(() => {
      try {
        ticketStore.removeDraft(row.id);
        refreshTick.value++;
        ElMessage.success('已删除');
      } catch (e) {
        ElMessage.error((e as Error).message);
      }
    })
    .catch(() => undefined);
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <span>我的工单</span>
      <el-tag size="small" style="margin-left: 12px">{{ CURRENT_USER }}</el-tag>
      <span class="hint">数据为原型内存模拟,检索支持单号 / 标题关键字</span>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane v-for="tab in TABS" :key="tab.key" :name="tab.key">
        <template #label>
          {{ tab.label }}
          <el-badge :value="counts[tab.key]" :type="tab.key === 'todo' ? 'danger' : 'info'" style="margin-left: 4px" />
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- 检索区 -->
    <div class="toolbar">
      <el-input v-model="filter.keyword" placeholder="单号 / 标题" clearable style="width: 200px" />
      <el-select v-model="filter.type" placeholder="类型" clearable style="width: 130px">
        <el-option v-for="(label, key) in TYPE_LABEL" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="filter.priority" placeholder="优先级" clearable style="width: 130px">
        <el-option v-for="(label, key) in PRIORITY_LABEL" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-if="activeTab !== 'drafts'" v-model="filter.status" placeholder="状态" clearable style="width: 130px">
        <el-option v-for="(label, key) in statusOptions" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <el-table :data="rows" stripe>
      <el-table-column label="单号" width="180">
        <template #default="{ row }">
          <el-link v-if="activeTab !== 'drafts'" type="primary" @click="router.push(`/tickets/${row.id}`)">{{ row.no }}</el-link>
          <span v-else>{{ row.no }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="TYPE_TAG[row.type as keyof typeof TYPE_TAG]" effect="plain">
            {{ TYPE_LABEL[row.type as keyof typeof TYPE_LABEL] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="优先级" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="PRIORITY_TAG[row.priority as keyof typeof PRIORITY_TAG]" effect="dark">
            {{ row.priority }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="95" align="center">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status as keyof typeof STATUS_TAG]">
            {{ STATUS_LABEL[row.status as keyof typeof STATUS_LABEL] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reporter" label="提单人" width="95" />
      <el-table-column label="当前处理" width="160">
        <template #default="{ row }">{{ row.assignee ?? row.assigneeGroup }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="165">
        <template #default="{ row }">{{ fmtTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" :width="activeTab === 'drafts' ? 190 : 90" fixed="right">
        <template #default="{ row }">
          <template v-if="activeTab === 'drafts'">
            <el-button link type="primary" @click="router.push({ path: '/tickets/create', query: { id: row.id } })">编辑</el-button>
            <el-button link type="success" @click="submitDraft(row)">提交</el-button>
            <el-button link type="danger" @click="deleteDraft(row)">删除</el-button>
          </template>
          <el-button v-else link type="primary" @click="router.push(`/tickets/${row.id}`)">
            {{ activeTab === 'todo' ? '处理' : '查看' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="rows.length === 0" description="暂无相关工单" />
  </el-card>
</template>

<style scoped>
.hint {
  color: #98a2b3;
  font-size: 12px;
  margin-left: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;
}
</style>

