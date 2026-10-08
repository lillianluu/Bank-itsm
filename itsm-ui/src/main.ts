import {createApp} from 'vue';
import {createPinia} from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';

const app = createApp(App);

// 全量注册 Element Plus 图标(原型阶段;后续按需引入优化体积)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(createPinia()).use(router).use(ElementPlus).mount('#app');

