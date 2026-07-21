<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.taskId" placeholder="任务ID" clearable style="width:140px" @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="load">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskId" label="任务ID" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : (row.status === 2 ? 'warning' : 'danger')" size="small">
              {{ row.status === 0 ? '成功' : (row.status === 2 ? '执行中' : '失败') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="result" label="结果" show-overflow-tooltip />
      </el-table>

      <el-pagination class="pager" background layout="prev,pager,next,total"
        :total="total" :page-size="query.size" @current-change="(p:number)=>{query.page=p;load()}" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listExecutions } from '@/api'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ taskId: '', status: undefined as number | undefined, page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const r = await listExecutions({
      taskId: query.taskId ? Number(query.taskId) : undefined,
      status: query.status,
      page: query.page,
      size: query.size
    })
    rows.value = r.data.data?.records ?? []
    total.value = r.data.data?.total ?? 0
  } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
