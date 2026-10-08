<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  MENU_TYPE_LABEL, MENU_TYPE_TAG, menuStore,
  type MenuNode, type MenuType
} from '../../api/admin';

const menuTree = ref<MenuNode[]>([]);
const expanded = ref(true);

function load(): void {
  menuTree.value = menuStore.tree();
}
onMounted(load);

/** 上级菜单选项:仅目录(M)与菜单(C)可作为父级 */
function toOptions(nodes: MenuNode[]): { value: number; label: string; disabled?: boolean; children?: unknown[] }[] {
  return nodes.map((n) => ({
    value: n.id,
    label: MENU_TYPE_LABEL[n.type] + ' - ' + n.name,
    disabled: n.type === 'F',
    children: n.children.length ? toOptions(n.children) : undefined
  }));
}
const parentOptions = computed(() => [{ value: 0, label: '根目录', children: toOptions(menuTree.value) }]);

// ===== 新增 / 编辑 =====
const dialog = reactive({ visible: false, isEdit: false, editingId: 0 });
const form = reactive<{
  parentId: number; name: string; type: MenuType; path: string;
  component: string; perm: string; icon: string; sort: number; visible: 0 | 1;
}>({ parentId: 0, name: '', type: 'C', path: '', component: '', perm: '', icon: '', sort: 1, visible: 1 });
const formRef = ref();

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  path: [{ required: true, message: '请输入路由地址', trigger: 'blur' }]
};

function openCreate(parentId = 0): void {
  dialog.isEdit = false;
  dialog.editingId = 0;
  Object.assign(form, { parentId, name: '', type: parentId === 0 ? 'M' : 'C', path: '', component: '', perm: '', icon: '', sort: 1, visible: 1 });
  dialog.visible = true;
}
function openEdit(row: MenuNode): void {
  dialog.isEdit = true;
  dialog.editingId = row.id;
  Object.assign(form, {
    parentId: row.parentId, name: row.name, type: row.type,
    path: row.path ?? '', component: row.component ?? '', perm: row.perm ?? '',
    icon: row.icon ?? '', sort: row.sort, visible: row.visible
  });
  dialog.visible = true;
}
function submit(): void {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      return;
    }
    const isBtn = form.type === 'F';
    const payload = {
      parentId: form.parentId,
      name: form.name.trim(),
      type: form.type,
      path: isBtn ? null : (form.path.trim() || null),
      component: form.type === 'C' ? (form.component.trim() || null) : null,
      perm: form.perm.trim() || null,
      icon: isBtn ? null : (form.icon.trim() || null),
      sort: form.sort,
      visible: isBtn ? (1 as const) : form.visible,
      status: 1 as const
    };
    try {
      if (dialog.isEdit) {
        menuStore.update(dialog.editingId, payload);
        ElMessage.success('已保存');
      } else {
        menuStore.create(payload);
        ElMessage.success('已创建');
      }
      dialog.visible = false;
      load();
    } catch (e) {
      ElMessage.error((e as Error).message);
    }
  });
}

// ===== 删除 / 展开折叠 =====
function onDelete(row: MenuNode): void {
  ElMessageBox.confirm(`确认删除菜单「${row.name}」?`, '删除确认', { type: 'warning' })
    .then(() => {
      try {
        menuStore.remove(row.id);
        ElMessage.success('已删除');
        load();
      } catch (e) {
        ElMessage.error((e as Error).message);
      }
    })
    .catch(() => undefined);
}

function toggleExpand(): void {
  expanded.value = !expanded.value;
}
</script>

<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button @click="toggleExpand">{{ expanded ? '折叠' : '展开' }}</el-button>
      <span class="hint">类型:目录 M / 菜单 C / 按钮 F(按钮仅用于权限标识,不渲染导航)</span>
      <div class="spacer" />
      <el-button type="primary" @click="openCreate(0)">新增菜单</el-button>
    </div>

    <el-table
      :data="menuTree"
      row-key="id"
      :default-expand-all="expanded"
      :key="expanded ? 'expand' : 'fold'"
      stripe
    >
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="MENU_TYPE_TAG[row.type as MenuType]" size="small">{{ MENU_TYPE_LABEL[row.type as MenuType] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由地址" min-width="130">
        <template #default="{ row }">{{ row.path ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="component" label="组件路径" min-width="150">
        <template #default="{ row }">{{ row.component ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="权限标识" min-width="170">
        <template #default="{ row }">
          <el-tag v-if="row.perm" size="small" type="info">{{ row.perm }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="可见" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.type !== 'F'" :type="row.visible === 1 ? 'success' : 'info'" size="small">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.type !== 'F'" link type="primary" @click="openCreate(row.id)">新增子级</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="muted tip">原型:M1 对接后按 /menus/tree 动态渲染左侧导航与按钮权限</div>
  </el-card>

  <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑菜单' : '新增菜单'" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="上级菜单">
        <el-tree-select v-model="form.parentId" :data="parentOptions" check-strictly style="width: 100%" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-radio-group v-model="form.type">
          <el-radio-button value="M">目录</el-radio-button>
          <el-radio-button value="C">菜单</el-radio-button>
          <el-radio-button value="F">按钮</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" maxlength="32" />
      </el-form-item>
      <el-form-item v-if="form.type !== 'F'" label="路由地址" prop="path">
        <el-input v-model="form.path" placeholder="如 /system/user" />
      </el-form-item>
      <el-form-item v-if="form.type === 'C'" label="组件路径">
        <el-input v-model="form.component" placeholder="如 system/user/index" />
      </el-form-item>
      <el-form-item label="权限标识">
        <el-input v-model="form.perm" placeholder="如 sys:user:add" />
      </el-form-item>
      <el-form-item v-if="form.type !== 'F'" label="图标">
        <el-input v-model="form.icon" placeholder="Element Plus 图标名,如 Setting" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="1" :max="999" />
      </el-form-item>
      <el-form-item v-if="form.type === 'C'" label="是否可见">
        <el-radio-group v-model="form.visible">
          <el-radio :value="1">显示</el-radio>
          <el-radio :value="0">隐藏</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog.visible = false">取消</el-button>
      <el-button type="primary" @click="submit">确定</el-button>
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
</style>

