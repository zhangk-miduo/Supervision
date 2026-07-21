<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.name" placeholder="机器人名称" clearable style="width:200px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openCreate">新建机器人</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="robotId" label="RobotId" width="160" />
        <el-table-column prop="webhookUrl" label="Webhook" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="test(row)">测试</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="prev,pager,next,total"
        :total="total" :page-size="query.size" @current-change="(p:number)=>{query.page=p;load()}" />
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑机器人' : '新建机器人'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：运维告警群" />
        </el-form-item>
        <el-form-item label="RobotId" required>
          <el-input v-model="form.robotId" placeholder="企业微信机器人 ID" />
        </el-form-item>
        <el-form-item label="Webhook" required>
          <el-input v-model="form.webhookUrl" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..." />
        </el-form-item>
        <el-form-item label="消息模板">
          <el-input v-model="form.template" type="textarea" :rows="3" placeholder="JSON 模板，可引用 ${var}" />
        </el-form-item>
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
import { listRobots, createRobot, updateRobot, deleteRobot, testRobot } from '@/api'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ name: '', page: 1, size: 10 })

const dialog = ref(false)
const saving = ref(false)
const form = reactive<any>({ id: 0, name: '', robotId: '', webhookUrl: '', template: '' })

async function load() {
  loading.value = true
  try {
    const r = await listRobots({ name: query.name, page: query.page, size: query.size })
    rows.value = r.data.data?.records ?? []
    total.value = r.data.data?.total ?? 0
  } finally { loading.value = false }
}
function openCreate() {
  Object.assign(form, { id: 0, name: '', robotId: '', webhookUrl: '', template: '' })
  dialog.value = true
}
function openEdit(row: any) {
  Object.assign(form, { ...row })
  dialog.value = true
}
async function save() {
  if (!form.name.trim() || !form.robotId.trim() || !form.webhookUrl.trim())
    return ElMessage.warning('请填写名称、RobotId 和 Webhook')
  saving.value = true
  try {
    const payload = { name: form.name, robotId: form.robotId, webhookUrl: form.webhookUrl, template: form.template }
    if (form.id) { await updateRobot(form.id, payload); ElMessage.success('已更新') }
    else { await createRobot(payload); ElMessage.success('已创建') }
    dialog.value = false
    load()
  } finally { saving.value = false }
}
async function test(row: any) {
  const r = await testRobot(row.id)
  if (r.data.code === 200) ElMessage.success('测试发送：' + r.data.data)
  else ElMessage.error(r.data.message)
}
async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该机器人？', '提示', { type: 'warning' })
  await deleteRobot(row.id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
