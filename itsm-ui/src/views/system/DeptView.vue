<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createDept, updateDept, deleteDept, fetchDeptTree, fetchLeaderOptions, type LeaderOption, type DeptNode } from '../../api/dept';
import http from '../../api/http';
import DeptEmployeeList from '../../components/DeptEmployeeList.vue';
const permissions = ref<string[]>([]);
const roles = ref<string[]>([]);
const can = (perm: string) => roles.value.includes('ADMIN') || permissions.value.includes(perm);
const loading = ref(false);
const saving = ref(false);

// ===== 树数据 =====
const deptTree = ref<DeptNode[]>([]);
const expanded = ref(false);
const selectedDept = ref<DeptNode | null>(null);
const departmentKeyword = ref('');
const appliedKeyword = ref('');
const employeeDept = ref<DeptNode | null>(null);
const employeeDialogVisible = ref(false);
function filterDepartments(nodes: DeptNode[], keyword: string): DeptNode[] {
  return nodes.flatMap(node => {
    if (node.name.toLocaleLowerCase().includes(keyword)) return [node];
    const children = filterDepartments(node.children, keyword);
    return children.length ? [{ ...node, children }] : [];
  });
}
const visibleDepartments = computed(() => appliedKeyword.value
  ? filterDepartments(deptTree.value, appliedKeyword.value.toLocaleLowerCase()) : deptTree.value);
function queryDepartments(): void {
  appliedKeyword.value = departmentKeyword.value.trim();
  selectedDept.value = null;
}
function showEmployees(row: DeptNode): void {
  employeeDept.value = row;
  employeeDialogVisible.value = true;
}

function toOptions(nodes: DeptNode[]): { value: string; label: string; disabled?: boolean; children?: unknown[] }[] {
  return nodes.map((n) => ({
    value: n.id,
    label: n.name,
    disabled: n.status === 0,
    children: n.children.length ? toOptions(n.children) : undefined
  }));
}

/** 上级部门选项:根(0) + 全部部门 */
const parentOptions = computed(() => [{ value: '0', label: '顶级部门', children: toOptions(deptTree.value) }]);

async function load(): Promise<void> {
  loading.value = true;
  try { deptTree.value = await fetchDeptTree(); selectedDept.value = null; }
  catch (e) { ElMessage.error((e as Error).message); }
  finally { loading.value = false; }
}
onMounted(async () => {
  try {
    const me = await http.get<never, { roles: string[]; perms: string[] }>('/auth/me');
    roles.value = me.roles;
    permissions.value = me.perms;
    if (can('sys:dept:list')) await load();
  } catch (e) { ElMessage.error((e as Error).message); }
});

// ===== 新增 / 编辑 =====
const dialog = reactive({ visible: false, isEdit: false, editingId: '' });
const form = reactive<{
  parentId: string; name: string; sort: number;
  leader: string; phone: string; email: string; status: 0 | 1;
}>({ parentId: '0', name: '', sort: 0, leader: '', phone: '', email: '', status: 1 });
const formRef = ref();

const rules = {
  name: [{ required: true, whitespace: true, message: '请输入部门名称', trigger: 'blur' }, { max: 64, message: '最多 64 个字符', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级部门', trigger: 'change' }]
};

const leaderOptions = ref<LeaderOption[]>([]);
const leaderLoading = ref(false);
const selectedLeader = ref('');
const legacyLeader = ref('');
let leaderRequest = 0;
async function searchLeaders(keyword = ''): Promise<void> {
  const request = ++leaderRequest;
  leaderLoading.value = true;
  try {
    const options = await fetchLeaderOptions(keyword);
    if (request === leaderRequest) leaderOptions.value = options;
  } catch (e) {
    if (request === leaderRequest) ElMessage.error((e as Error).message);
  } finally {
    if (request === leaderRequest) leaderLoading.value = false;
  }
}
function chooseLeader(username: string): void {
  if (!username) { form.leader = ''; return; }
  if (username === `legacy:${legacyLeader.value}`) { form.leader = legacyLeader.value; return; }
  const user = leaderOptions.value.find(u => u.username === username);
  if (user) form.leader = user.nickname;
}
function resetLeader(leader = ''): void {
  selectedLeader.value = leader ? `legacy:${leader}` : '';
  legacyLeader.value = leader;
  leaderOptions.value = [];
  void searchLeaders();
}

function openCreate(parentId = '0'): void {
  dialog.isEdit = false;
  dialog.editingId = '';
  Object.assign(form, { parentId, name: '', sort: 0, leader: '', phone: '', email: '', status: 1 });
  resetLeader(form.leader);
  dialog.visible = true;
  formRef.value?.clearValidate();
}
function openEdit(row: DeptNode): void {
  dialog.isEdit = true;
  dialog.editingId = row.id;
  Object.assign(form, {
    parentId: row.parentId, name: row.name, sort: row.sort,
    leader: row.leader ?? '', phone: row.phone ?? '', email: row.email ?? '', status: row.status
  });
  resetLeader(form.leader);
  dialog.visible = true;
  formRef.value?.clearValidate();
}
function submit(): void {
  if (saving.value) return;
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }
    const payload = {
      parentId: form.parentId, name: form.name.trim(), sort: form.sort,
      leader: form.leader.trim() || null, phone: form.phone.trim() || null,
      email: form.email.trim() || null, status: form.status
    };
    saving.value = true;
    try {
      if (dialog.isEdit) {
        await updateDept(dialog.editingId, payload);
        ElMessage.success('已保存');
      } else {
        await createDept(payload);
        ElMessage.success('已创建');
      }
      dialog.visible = false;
      await load();
    } catch (e) {
      ElMessage.error((e as Error).message);
    } finally { saving.value = false; }
  });
}

// ===== 删除 / 展开折叠 =====
function onDelete(row: DeptNode): void {
  ElMessageBox.confirm(`确认删除部门「${row.name}」?`, '删除确认', { type: 'warning' })
    .then(async () => {
      try {
        await deleteDept(row.id);
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
      <el-input v-model="departmentKeyword" placeholder="请输入部门名称" clearable style="width: 260px"
        @keyup.enter="queryDepartments" @clear="queryDepartments" />
      <div class="spacer" />
      <el-button v-if="can('sys:dept:list')" @click="queryDepartments">查询</el-button>
      <el-button v-if="can('sys:dept:add')" type="primary" @click="openCreate(selectedDept?.id ?? '0')" :disabled="selectedDept?.status === 0">新增部门</el-button>
    </div>

    <el-table
      :data="visibleDepartments"
      row-key="id"
      highlight-current-row
      @current-change="(row: DeptNode | null) => selectedDept = row"
      :default-expand-all="expanded || !!appliedKeyword"
      :key="`${expanded}-${appliedKeyword}`"
      v-loading="loading"
      stripe
    >
      <el-table-column prop="name" label="部门名称" min-width="220" />
      <el-table-column prop="leader" label="负责人" width="110">
        <template #default="{ row }">{{ row.leader ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="员工清单" width="120">
        <template #default="{ row }"><el-button link type="primary" @click="showEmployees(row)">查看员工</el-button></template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('sys:dept:add')" link type="primary" :disabled="row.status === 0" @click="openCreate(row.id)">新增子级</el-button>
          <el-button v-if="can('sys:dept:edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="can('sys:dept:delete')" link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !visibleDepartments.length" :description="appliedKeyword ? '未找到匹配部门' : '暂无部门，请新增部门建立组织架构'" />
  </el-card>

  <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑部门' : '新增部门'" width="800px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="上级部门" prop="parentId">
        <el-tree-select v-model="form.parentId" :data="parentOptions" :disabled="dialog.isEdit" filterable check-strictly style="width: 100%" />
      </el-form-item>
      <el-form-item label="部门名称" prop="name">
        <el-input v-model="form.name" maxlength="64" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" :max="999" />
      </el-form-item>
      <el-form-item label="负责人">
        <el-select
          v-model="selectedLeader" filterable remote clearable
          :remote-method="searchLeaders" :loading="leaderLoading"
          placeholder="按姓名、邮箱或手机号搜索" style="width: 100%"
          @change="chooseLeader" @visible-change="(visible: boolean) => visible && searchLeaders()"
        >
          <el-option v-if="legacyLeader" :value="`legacy:${legacyLeader}`" :label="legacyLeader" />
          <el-option v-for="user in leaderOptions" :key="user.id" :value="user.username" :label="user.nickname" class="dept-leader-option">
            <div>{{ user.nickname }} <span class="muted">（{{ user.username }}）</span></div>
            <div class="muted">{{ [user.email, user.phone].filter(Boolean).join(' · ') }}</div>
          </el-option>
        </el-select>
        <div class="tip muted">显示最多 50 人，可输入关键词缩小范围</div>
      </el-form-item>
      <el-form-item label="员工清单">
        <DeptEmployeeList v-if="dialog.visible && dialog.isEdit" :key="dialog.editingId" :dept-id="dialog.editingId" style="width: 100%" />
        <span v-else class="muted">保存部门后，可在用户管理中为员工分配部门</span>
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
      <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
    </template>
  </el-dialog>
  <el-dialog v-model="employeeDialogVisible" title="部门信息" width="800px">
    <template v-if="employeeDept && employeeDialogVisible">
      <el-descriptions :column="2" border style="margin-bottom: 20px">
        <el-descriptions-item label="部门名称">{{ employeeDept.name }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ employeeDept.leader || '-' }}</el-descriptions-item>
      </el-descriptions>
      <DeptEmployeeList :key="employeeDept.id" :dept-id="employeeDept.id" />
    </template>
  </el-dialog>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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


<style>
.dept-leader-option { height: auto; min-height: 34px; line-height: 22px; padding-top: 5px; padding-bottom: 5px; }
</style>
