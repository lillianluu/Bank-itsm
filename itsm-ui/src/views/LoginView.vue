<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import http, { setToken } from '../api/http';

/** 登录响应(与后端 LoginVO 对齐) */
interface LoginVO {
  accessToken: string;
  refreshToken: string;
  userId: number;
  username: string;
  nickname: string;
  roles: string[];
}

const router = useRouter();
const username = ref('');
const password = ref('');
const loading = ref(false);

async function onLogin() {
  if (!username.value.trim() || !password.value) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  loading.value = true;
  try {
    const vo = await http.post<never, LoginVO>('/auth/login', {
      username: username.value.trim(),
      password: password.value
    });
    setToken(vo.accessToken, vo.refreshToken);
    ElMessage.success(`欢迎,${vo.nickname}`);
    await router.push('/');
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败');
  } finally {
    loading.value = false;
  }
}

/** 原型演示:M2 工单域页面使用内存 mock 数据,无需后端 */
function enterPrototype(): void {
  router.push('/workbench');
}
</script>

<template>
  <div class="login-page">
    <!-- 背景装饰:网格 + 光晕 -->
    <div class="bg-grid" />
    <div class="glow glow-1" />
    <div class="glow glow-2" />

    <el-card class="login-card" shadow="never">
      <div class="brand">
        <div class="brand-mark">IT</div>
        <h1>IT服务管理平台</h1>
        <p class="slogan">统一服务门户 · ITIL 流程驱动 · SLA 全程度量</p>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="用户名 / 工号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            show-password
            placeholder="密码"
            size="large"
            @keyup.enter="onLogin"
          />
        </el-form-item>
        <el-button class="login-btn" type="primary" size="large" :loading="loading" @click="onLogin">
          登 录
        </el-button>
        <el-button class="proto-btn" size="large" @click="enterPrototype">
          跳过登录,进入原型演示(M2 工单域)
        </el-button>
      </el-form>

      <div class="foot">© 2026 IT服务管理平台 · 私有化部署版</div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  overflow: hidden;
  /* 深空科技蓝渐变底 */
  background:
    radial-gradient(ellipse at 20% 10%, rgba(23, 92, 255, 0.35) 0%, transparent 50%),
    radial-gradient(ellipse at 85% 90%, rgba(0, 210, 255, 0.25) 0%, transparent 45%),
    linear-gradient(135deg, #060d1f 0%, #0a1a3a 55%, #0d2450 100%);
}

/* 细网格线 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(80, 160, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(80, 160, 255, 0.07) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse at center, #000 30%, transparent 75%);
}

/* 漂浮光晕 */
.glow {
  position: absolute;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.35;
  animation: float 12s ease-in-out infinite alternate;
}

.glow-1 {
  top: -140px;
  left: -120px;
  background: radial-gradient(circle, #1668dc 0%, transparent 70%);
}

.glow-2 {
  bottom: -160px;
  right: -120px;
  background: radial-gradient(circle, #00c8ff 0%, transparent 70%);
  animation-delay: -6s;
}

@keyframes float {
  from { transform: translateY(-20px) scale(1); }
  to { transform: translateY(30px) scale(1.12); }
}

/* 玻璃拟态登录卡 */
.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 8px 10px;
  border-radius: 14px;
  background: rgba(13, 27, 62, 0.55);
  border: 1px solid rgba(80, 160, 255, 0.35);
  box-shadow: 0 0 60px rgba(20, 110, 255, 0.25), inset 0 0 30px rgba(64, 158, 255, 0.06);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

:deep(.el-card__body) {
  padding: 28px 28px 16px;
}

.brand {
  text-align: center;
  margin-bottom: 22px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-weight: 700;
  font-size: 18px;
  color: #fff;
  background: linear-gradient(135deg, #1677ff 0%, #00c8ff 100%);
  box-shadow: 0 0 24px rgba(0, 170, 255, 0.6);
}

h1 {
  margin: 0;
  font-size: 22px;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #e8f3ff 20%, #7cc4ff 60%, #00d2ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.slogan {
  margin: 8px 0 0;
  font-size: 12px;
  color: rgba(160, 200, 255, 0.65);
  letter-spacing: 1px;
}

/* 深色输入框适配 */
:deep(.el-form-item__label) {
  color: rgba(190, 220, 255, 0.85);
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid transparent;
  box-shadow: 0 0 0 1px rgba(80, 160, 255, 0.25) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset, 0 0 12px rgba(64, 158, 255, 0.35);
}

:deep(.el-input__inner) {
  color: #eaf4ff;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(150, 180, 220, 0.45);
}

.login-btn {
  width: 100%;
  margin: 6px 0 0;
  border: none;
  color: #fff;
  background: linear-gradient(90deg, #1677ff 0%, #00a6ff 100%);
  box-shadow: 0 4px 18px rgba(0, 140, 255, 0.45);
  letter-spacing: 6px;
}

.login-btn:hover {
  opacity: 0.92;
}

.proto-btn {
  width: 100%;
  margin: 12px 0 0 !important;
  color: rgba(160, 200, 255, 0.9);
  background: transparent;
  border: 1px solid rgba(80, 160, 255, 0.35);
}

.proto-btn:hover {
  color: #7cc4ff;
  border-color: rgba(80, 160, 255, 0.7);
  background: rgba(80, 160, 255, 0.08);
}

.foot {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: rgba(140, 175, 220, 0.4);
}
</style>

