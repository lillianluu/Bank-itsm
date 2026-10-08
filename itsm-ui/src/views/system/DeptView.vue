<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deptStore, type Dept, type DeptNode } from '../../api/admin';

// ===== 树数据 =====
const deptTree = ref<DeptNode[]>([]);
const expanded = ref(true);

function toOptions(nodes: DeptNode[]): { value: number; label: string; children?: unknown[] }[] {
  return nodes.map((n) => ({
    value: n.id,
    label: n.name,
    children: n.children.length ? toOptions(n.children) : undefined
  }));
}

/** 上级部门选项:根(0) + 全部部门 */
const parentOptions = computed(() => [{ value: 0, label: '顶级部门', children: toOptions(deptTree.value) }]);

function load(): void {
  deptTree.value = deptStore.tree();
}
onMounted(load);

// ===== 新增 / 编辑 =====
const dialog = reactive({ visible: false, isEdit: false, editingId: 0 });
const form = reactive<{
  parentId: number; name: string; sort: number;
  leader: string; phone: string; email: string; status: 0 | 1;
}>({ parentId: 0, name: '', sort: 1, leader: '', phone: '', email: '', status: 1 });
const formRef = ref();

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级部门', trigger: 'change' }]
};

function openCreate(parentId = 0): void {
  dialog.isEdit = false;
  dialog.editingId = 0;
  Object.assign(form, { parentId, name: '', sort: 1, leader: '', phone: '', email: '', status: 1 });
  dialog.visible = true;
}
function openEdit(row: Dept): void {
  dialog.isEdit = true;
  dialog.editingId = row.id;
  Object.assign(form, {
    parentId: row.parentId, name: row.name, sort: row.sort,
    leader: row.leader ?? '', phone: row.phone ?? '', email: row.email ?? '', status: row.status
  });
  dialog.visible = true;
}
function submit(): void {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      return;
    }
    const payload = {
      parentId: form.parentId, name: form.name.trim(), sort: form.sort,
      leader: form.leader.trim() || null, phone: form.phone.trim() || null,
      email: form.email.trim() || null, status: form.status
    };
    try {
      if (dialog.isEdit) {
        deptStore.update(dialog.editingId, payload);
        ElMessage.success('已保存');
      } else {
        deptStore.create(payload);
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
function onDelete(row: Dept): void {
  ElMessageBox.confirm(`确认删除部门「${row.name}」?`, '删除确认', { type: 'warning' })
    .then(() => {
      try {
        deptStore.remove(row.id);
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
      <div class="spacer" />
      <el-button type="primary" @click="openCreate(0)">新增部门</el-button>
    </div>

    <el-table
      :data="deptTree"
      row-key="id"
      :default-expand-all="expanded"
      :key="expanded ? 'expand' : 'fold'"
      v-loading="false"
      stripe
    >
      <el-table-column prop="name" label="部门名称" min-width="220" />
      <el-table-column prop="leader" label="负责人" width="110">
        <template #default="{ row }">{{ row.leader ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140">
        <template #default="{ row }">{{ row.phone ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="150">
        <template #default="{ row }">{{ row.email ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCreate(row.id)">新增子级</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="row.id === 1" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="muted tip">原型:有下级部门或存在用户占用时不可删除(接入后端后由接口校验)</div>
  </el-card>

  <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑部门' : '新增部门'" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="上级部门" prop="parentId">
        <el-tree-select v-model="form.parentId" :data="parentOptions" check-strictly style="width: 100%" />
      </el-form-item>
      <el-form-item label="部门名称" prop="name">
        <el-input v-model="form.name" maxlength="32" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="1" :max="999" />
      </el-form-item>
      <el-form-item label="负责人">
        <el-input v-model="form.leader" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
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

