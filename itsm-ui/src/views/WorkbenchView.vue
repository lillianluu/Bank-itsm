<script setup lang="ts">
import { onMounted, ref } from 'vue';
import http from '../api/http';

interface Overview {
  todayNew: number;
  pending: number;
  processing: number;
  overdue: number;
}

const overview = ref<Overview>({ todayNew: 0, pending: 0, processing: 0, overdue: 0 });

onMounted(async () => {
  try {
    overview.value = await http.get<never, Overview>('/dashboard/overview');
  } catch {
    // 骨架阶段:后端未启动时静默降级
  }
});
</script>

<template>
  <div style="padding: 24px">
    <h2>服务台看板</h2>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card><el-statistic title="今日新建" :value="overview.todayNew" /></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><el-statistic title="待受理" :value="overview.pending" /></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><el-statistic title="处理中" :value="overview.processing" /></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><el-statistic title="已超期" :value="overview.overdue" /></el-card>
      </el-col>
    </el-row>
  </div>
</template>

