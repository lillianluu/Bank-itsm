<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  ticketStore, slaRemainingMs, TYPE_LABEL, TYPE_TAG, STATUS_LABEL, STATUS_TAG,
  PRIORITY_LABEL, PRIORITY_TAG
} from '../../api/ticket';
import type { Ticket, TicketType, TicketStatus, Priority } from '../../api/ticket';

const router = useRouter();

// ===== 筛选条件(原型:前端内存过滤;后端:GET /tickets 传参) =====
const filter = ref<{ type?: TicketType; status?: TicketStatus; priority?: Priority; keyword?: string }>({});
const rows = computed(() => ticketStore.list(filter.value));

// ===== 前端分页 =====
const page = ref(1);
const pageSize = ref(10);
const paged = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value));

function resetFilter(): void {
  filter.value = {};
  page.value = 1;
}

function fmtTime(v: string): string {
  return new Date(v).toLocaleString('zh-CN', { hour12: false });
}

/** SLA 剩余展示 */
function slaText(t: Ticket): string {
  const ms = slaRemainingMs(t);
  const h = Math.abs(ms) / 3600_000;
  const label = h >= 1 ? `${h.toFixed(1)} 小时` : `${Math.round(Math.abs(ms) / 60_000)} 分钟`;
  return ms >= 0 ? `剩余 ${label}` : `超时 ${label}`;
}

function slaTag(t: Ticket): string {
  const ms = slaRemainingMs(t);
  if (t.status === 'CLOSED') return 'info';
  if (ms < 0) return 'danger';
  if (ms < 3600_000) return 'warning';
  return 'success';
}

function openDetail(t: Ticket): void {
  router.push(`/tickets/${t.id}`);
}
</script>

<template>
  <div>
    <!-- 筛选区 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="类型">
          <el-select v-model="filter.type" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="(label, key) in TYPE_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="(label, key) in STATUS_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="filter.priority" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="(label, key) in PRIORITY_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="filter.keyword"
            placeholder="单号 / 标题"
            clearable
            style="width: 200px"
            @keyup.enter="page = 1"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" plain @click="page = 1">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
        <el-form-item style="float: right">
          <el-button type="primary" @click="router.push('/tickets/create')">＋ 新建工单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="paged" stripe @row-click="openDetail">
        <el-table-column label="单号" width="180">
          <template #default="{ row }">
            <el-link type="primary" @click.stop="openDetail(row)">{{ row.no }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="TYPE_TAG[row.type as keyof typeof TYPE_TAG]" effect="plain">
              {{ TYPE_LABEL[row.type as keyof typeof TYPE_LABEL] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="优先级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="PRIORITY_TAG[row.priority as keyof typeof PRIORITY_TAG]" effect="dark">
              {{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status as keyof typeof STATUS_TAG]">
              {{ STATUS_LABEL[row.status as keyof typeof STATUS_LABEL] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporter" label="提单人" width="100" />
        <el-table-column label="处理组/人" width="150">
          <template #default="{ row }">
            {{ row.assigneeGroup }}<template v-if="row.assignee"> / {{ row.assignee }}</template>
          </template>
        </el-table-column>
        <el-table-column label="SLA 剩余" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="slaTag(row)" effect="plain">{{ slaText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="rows.length"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>

