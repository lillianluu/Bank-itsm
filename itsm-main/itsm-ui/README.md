# ITSM 前端(itsm-ui)

ITSM 一体化 IT 服务管理平台的 Web 前端:基于 **Vue 3.4 + TypeScript + Vite 5 + Element Plus 2.7 + Pinia + Vue Router**。

后端工程见 `../itsm-server/`(Spring Boot,默认端口 8080);开发环境通过 Vite 代理转发 `/api`,前端与后端同源,无需处理跨域。

## 一、环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| Node.js | ≥ 18 | Vite 5 最低要求 |
| npm | ≥ 9 | 或使用 pnpm/yarn(自行适配锁文件) |

## 二、目录结构

```
itsm-ui/
├── index.html                 入口 HTML(SPA 挂载点 #app)
├── package.json               依赖与脚本
├── vite.config.ts             Vite 配置:vue 插件、@ 别名、/api 开发代理
├── tsconfig.json / tsconfig.node.json   TypeScript 配置(strict 模式)
└── src/
    ├── main.ts                应用入口:挂载 Pinia / Router / Element Plus
    ├── App.vue                根组件(仅 router-view + 全局样式)
    ├── env.d.ts               Vite 客户端类型 + *.vue 模块声明
    ├── router/
    │   └── index.ts           路由:/login 登录页、/ 服务台工作台(均懒加载)
    ├── api/
    │   └── http.ts            axios 实例:baseURL=/api/v1,响应拦截器自动解包 Result
    └── views/
        ├── LoginView.vue      登录页占位(表单 → POST /auth/login)
        └── WorkbenchView.vue  服务台看板占位(GET /dashboard/overview 四项统计)
```

## 三、快速开始

```bash
# 安装依赖
npm install

# 本地开发(默认 http://localhost:5173)
npm run dev

# 生产构建(产物在 dist/)
npm run build

# 本地预览构建产物
npm run preview
```

联调前先启动后端:

```bash
cd ../itsm-server
mvn -DskipTests package && java -jar itsm-boot/target/itsm-boot.jar
# 或:docker compose -f itsm-server/deploy/docker/docker-compose.yml up -d
```

## 四、关键约定

### 1. 统一 API 调用层(`src/api/http.ts`)

- 所有请求经 `http` 实例发出,`baseURL = '/api/v1'`;
- 后端统一响应结构为 `Result<T> = { code, message, data, traceId }`(`code === 0` 表示成功);
- 响应拦截器**自动解包**:`await http.get(...)` 直接拿到 `data` 业务数据;非 0 code 或网络错误统一 reject `Error(message)`,页面侧只需 `try/catch` 展示;

```ts
import http from '@/api/http';

const overview = await http.get<never, Overview>('/dashboard/overview');
```

### 2. 路径别名

`@` 指向 `src/`(vite.config.ts 与 tsconfig.json 已同步配置):

```ts
import http from '@/api/http';
```

### 3. 开发代理(`vite.config.ts`)

```ts
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
});
```

后端地址变更时只改这一处;生产环境由 nginx / 网关承担同样的 `/api` 反向代理。

### 4. UI 组件

Element Plus 全量引入(`main.ts`),可直接使用 `el-*` 组件;后续体积优化可切换为 unplugin 按需引入。

## 五、TODO(V1.0 演进方向)

1. **登录态**:对接 JWT(登录后存储 token,`http.ts` 请求拦截器统一注入 `Authorization: Bearer xxx`,401 时跳转登录页);
2. **工单**:列表(筛选/分页)、详情(表单 + 时间线 + 流程图)、创建(动态表单渲染,Schema 由 `/catalog-items` 提供)、处理动作(受理/转派/挂起/解决/关闭);
3. **服务目录**:目录树与条目管理页;
4. **知识库**:文章列表/详情/检索;
5. **报表**:运营看板图表(ECharts)、预置报表页;
6. **布局骨架**:顶栏 + 侧边菜单(按 RBAC 权限渲染菜单);
7. **部署**:`Dockerfile`(node 构建 → nginx 托管 dist)+ 纳入 `itsm-server/deploy/docker/docker-compose.yml`。

