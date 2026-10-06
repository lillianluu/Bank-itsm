<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import http from '../api/http';

const router = useRouter();
const username = ref('');
const password = ref('');
const loading = ref(false);

async function onLogin() {
  loading.value = true;
  try {
    // TODO(V1.0): 对接 JWT 登录,token 存储后统一注入 Authorization 头
    await http.post('/auth/login', { username: username.value, password: password.value });
    await router.push('/');
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>ITSM 服务台</h2>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="用户名 / 工号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password placeholder="密码" />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.login-card {
  width: 360px;
  padding: 8px 12px;
}
h2 {
  text-align: center;
  margin: 8px 0 20px;
}
</style>

