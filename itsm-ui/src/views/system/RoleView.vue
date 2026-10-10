<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type ElTree } from 'element-plus';
import {
  BUILTIN_ADMIN, DATA_SCOPE_LABEL, assignRoleMenus, createRole, deleteRole,
  fetchMenuTree, listRoles, updateRole,
  type DataScope, type MenuTreeNode, type Role, type RoleSaveCmd
} from '../../api/role';

const rows = ref<Role[]>([]);
const loading = ref(false);

async function load(): Promise<void> {
  loading.value = true;
  try {
    rows.value = await listRoles();
  } catch (e) {
    ElMessage.error((e as Error).message);
  } finally {
    loading.value = false;
  }
}
onMounted(load);

// ===== 新增 / 编辑 =====
const dialog = reactive({ visible: false, isEdit: false, editingId: 0, codeLocked: false, saving: false });
const form = reactive<{
  name: string; code: string; dataScope: DataScope; status: 0 | 1; remark: string;
}>({ name: '', code: '', dataScope: 'SELF', status: 1, remark: '' });
const formRef = ref();

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z0-9_]{2,32}$/, message: '大写字母/数字/下划线', trigger: 'blur' }]
};

function openCreate(): void {
  dialog.isEdit = false;
  dialog.editingId = 0;
  dialog.codeLocked = false;
  Object.assign(form, { name: '', code: '', dataScope: 'SELF', status: 1, remark: '' });
  dialog.visible = true;
}
function openEdit(row: Role): void {
  dialog.isEdit = true;
  dialog.editingId = row.id;
  dialog.codeLocked = row.code === BUILTIN_ADMIN;
  Object.assign(form, { name: row.name, code: row.code, dataScope: row.dataScope, status: row.status, remark: row.remark ?? '' });
  dialog.visible = true;
}
function buildCmd(): RoleSaveCmd {
  return {
    name: form.name.trim(), code: form.code.trim(),
    dataScope: form.dataScope, status: form.status, remark: form.remark.trim() || null
  };
}
function submit(): void {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }
    dialog.saving = true;
    try {
      if (dialog.isEdit) {
        await updateRole(dialog.editingId, buildCmd());
        ElMessage.success('已保存');
      } else {
        await createRole(buildCmd());
        ElMessage.success('已创建');
      }
      dialog.visible = false;
      await load();
    } catch (e) {
      ElMessage.error((e as Error).message);
    } finally {
      dialog.saving = false;
    }
  });
}

// ===== 分配菜单 =====
const grantDialog = reactive({ visible: false, roleId: 0, roleName: '', saving: false });
const treeRef = ref<InstanceType<typeof ElTree>>();
const menuTree = ref<MenuTreeNode[]>([]);
/** 树中非叶子(目录/菜单)节点 id:回显时需剔除,避免父节点全选放大授权 */
const treeParentIds = new Set<number>();

function collectParentIds(nodes: MenuTreeNode[]): void {
  nodes.forEach((n) => {
    if (n.children?.length) {
      treeParentIds.add(n.id);
      collectParentIds(n.children);
    }
  });
}

async function openGrant(row: Role): Promise<void> {
  grantDialog.roleId = row.id;
  grantDialog.roleName = row.name;
  grantDialog.visible = true;
  try {
    menuTree.value = await fetchMenuTree();
  } catch (e) {
    ElMessage.error((e as Error).message);
    return;
  }
  treeParentIds.clear();
  collectParentIds(menuTree.value);
  await nextTick();
  // 仅勾选叶子(按钮/无子菜单):父节点由 el-tree 自动半选,
  // 提交时合并 checked + halfChecked,与后端"全集存库"一致
  const leafIds = row.menuIds.filter((id) => !treeParentIds.has(id));
  treeRef.value?.setCheckedKeys(leafIds);
}
async function submitGrant(): Promise<void> {
  const checked = treeRef.value?.getCheckedKeys(false) as number[] ?? [];
  const half = treeRef.value?.getHalfCheckedKeys() as number[] ?? [];
  grantDialog.saving = true;
  try {
    await assignRoleMenus(grantDialog.roleId, [...checked, ...half]);
    grantDialog.visible = false;
    ElMessage.success(`已保存「${grantDialog.roleName}」的菜单权限`);
    await load();
  } catch (e) {
    ElMessage.error((e as Error).message);
  } finally {
    grantDialog.saving = false;
  }
}

// ===== 删除 =====
function onDelete(row: Role): void {
  ElMessageBox.confirm(`确认删除角色「${row.name}」?删除后不可在列表中恢复。`, '删除确认', { type: 'warning' })
    .then(async () => {
      try {
        await deleteRole(row.id);
        ElMessage.success('已删除');
        await load();
      } catch (e) {
        ElMessage.error((e as Error).message);
      }
    })
    .catch(() => undefined);
}
</script>

<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="hint">角色决定"能访问什么菜单 + 数据权限范围";内置 ADMIN 由代码放行、不可删除</span>
      <div class="spacer" />
      <el-button type="primary" @click="openCreate">新增角色</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="角色名称" min-width="140" />
      <el-table-column label="角色编码" min-width="120">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.code }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="数据权限" width="110">
        <template #default="{ row }">{{ DATA_SCOPE_LABEL[row.dataScope as DataScope] }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180">
        <template #default="{ row }">{{ row.remark ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openGrant(row)">分配菜单</el-button>
          <el-button link type="danger" :disabled="row.code === BUILTIN_ADMIN" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新增 / 编辑 -->
  <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑角色' : '新增角色'" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="form.name" maxlength="32" />
      </el-form-item>
      <el-form-item label="角色编码" prop="code">
        <el-input v-model="form.code" :disabled="dialog.codeLocked" placeholder="如 OPS_ENGINEER" />
      </el-form-item>
      <el-form-item label="数据权限">
        <el-select v-model="form.dataScope" style="width: 100%">
          <el-option label="仅本人" value="SELF" />
          <el-option label="本部门" value="DEPT" />
          <el-option label="全部数据" value="ALL" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="dialog.saving" @click="submit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 分配菜单 -->
  <el-dialog v-model="grantDialog.visible" :title="`分配菜单 - ${grantDialog.roleName}`" width="480px">
    <el-tree
      v-loading="!menuTree.length"
      ref="treeRef"
      :data="menuTree"
      node-key="id"
      show-checkbox
      default-expand-all
      :props="{ label: 'name', children: 'children' }"
    />
    <template #footer>
      <el-button @click="grantDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="grantDialog.saving" @click="submitGrant">确定</el-button>
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
</style>

