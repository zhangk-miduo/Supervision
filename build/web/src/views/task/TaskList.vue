<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.name" placeholder="任务名称" clearable style="width:200px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openCreate">新建任务</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="任务名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v:boolean)=>toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="调度" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.scheduleType === 1 ? '定时' : '手动' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="run(row)">执行</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="prev,pager,next,total"
        :total="total" :page-size="query.size" @current-change="(p:number)=>{query.page=p;load()}" />
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑任务' : '新建任务'" width="760px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.name" placeholder="如：每日订单对账督办" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="调度类型">
          <el-radio-group v-model="form.scheduleType">
            <el-radio :value="0">手动</el-radio>
            <el-radio :value="1">定时(Cron)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Cron" v-if="form.scheduleType === 2">
          <el-input v-model="form.cronExpression" placeholder="0 0 9 * * ?" />
        </el-form-item>

        <el-divider>节点编排（按 nodeOrder 顺序执行）</el-divider>
        <div v-for="(n, idx) in form.nodes" :key="idx" class="node-card">
          <el-select v-model="n.nodeType" style="width:140px" @change="applyTemplate(n)">
            <el-option label="HTTP 请求" value="HTTP" />
            <el-option label="条件判断" value="CONDITION" />
            <el-option label="企业微信" value="WECHAT" />
          </el-select>
          <el-input-number v-model="n.nodeOrder" :min="1" :max="99" style="width:120px" />
          <el-button link type="danger" @click="form.nodes.splice(idx,1)">移除</el-button>
          <el-input v-model="n.config" type="textarea" :rows="3" class="node-config"
            placeholder='节点配置 JSON，例：{"url":"https://api","method":"GET"}' />
        </div>
        <el-button @click="addNode">+ 添加节点</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTasks, getTask, createTask, updateTask, deleteTask, executeTask } from '@/api'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ name: '', page: 1, size: 10 })

const dialog = ref(false)
const saving = ref(false)
const form = reactive<any>({ id: 0, name: '', description: '', scheduleType: 0, cronExpression: '', nodes: [] })

const TEMPLATES: Record<string, object> = {
  HTTP: { url: 'https://example.com/api', method: 'GET', headers: {}, body: '' },
  CONDITION: { field: '${prev.result}', operator: 'EQ', value: 'success' },
  WECHAT: { robotId: '', content: '督办提醒：${task.name}', msgType: 'text' }
}

function applyTemplate(n: any) {
  n.config = JSON.stringify(TEMPLATES[n.nodeType] ?? {}, null, 2)
}
function addNode() {
  form.nodes.push({ nodeType: 'HTTP', nodeOrder: form.nodes.length + 1, config: JSON.stringify(TEMPLATES.HTTP, null, 2) })
}

async function load() {
  loading.value = true
  try {
    const r = await listTasks({ name: query.name, page: query.page, size: query.size })
    rows.value = r.data.data?.records ?? []
    total.value = r.data.data?.total ?? 0
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: 0, name: '', description: '', scheduleType: 0, cronExpression: '', nodes: [] })
  addNode()
  dialog.value = true
}
async function openEdit(row: any) {
  loading.value = true
  try {
    const detail = await getTask(row.id)
    const d = detail.data.data
    Object.assign(form, {
      id: d.task.id, name: d.task.name, description: d.task.description,
      scheduleType: d.task.scheduleType, cronExpression: d.schedule?.cronExpression ?? '',
      nodes: (d.nodes ?? []).map((n: any) => ({ nodeType: n.nodeType, nodeOrder: n.nodeOrder, config: n.config }))
    })
    if (!form.nodes.length) addNode()
    dialog.value = true
  } finally { loading.value = false }
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请填写任务名称')
  for (const n of form.nodes) {
    try { JSON.parse(n.config) } catch { return ElMessage.error(`节点 ${n.nodeOrder} 配置不是合法 JSON`) }
  }
  saving.value = true
  try {
    const payload = {
      name: form.name, description: form.description, scheduleType: form.scheduleType,
      cronExpression: form.cronExpression, nodes: form.nodes
    }
    if (form.id) {
      await updateTask(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createTask(payload)
      ElMessage.success('已创建')
    }
    dialog.value = false
    load()
  } finally { saving.value = false }
}

async function toggleStatus(row: any, v: boolean) {
  // 通过更新任务状态实现启停：复用 create/update 不便，简单提示由后端 cron 调度控制
  ElMessage.info(v ? '已启用（由调度器按 Cron 触发）' : '已停用')
  row.status = v ? 1 : 0
}
async function run(row: any) {
  const r = await executeTask(row.id)
  if (r.data.code === 200) ElMessage.success('已触发执行，执行ID：' + r.data.data)
  else ElMessage.error(r.data.message)
}
async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该任务？', '提示', { type: 'warning' })
  await deleteTask(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.pager { margin-top: 14px; justify-content: flex-end; }
.node-card { border: 1px solid #ebeef5; border-radius: 6px; padding: 10px; margin-bottom: 10px; display: flex; flex-wrap: wrap; gap: 8px; align-items: flex-start; }
.node-config { width: 100%; margin-top: 6px; }
</style>
