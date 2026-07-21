<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-title">任务总数</div>
          <div class="stat-value">{{ stats.taskCount }}</div>
          <div class="stat-sub">已启用 {{ stats.taskEnabled }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-title">机器人</div>
          <div class="stat-value">{{ stats.robotCount }}</div>
          <div class="stat-sub">企业微信机器人</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-title">执行总数</div>
          <div class="stat-value">{{ stats.execCount }}</div>
          <div class="stat-sub">累计调度执行</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-title">成功率</div>
          <div class="stat-value">{{ successRate }}%</div>
          <div class="stat-sub">最近 100 次执行</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>最近执行</template>
      <el-table :data="recent" v-loading="loading" size="small">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskId" label="任务ID" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : (row.status === 2 ? 'warning' : 'danger')" size="small">
              {{ row.status === 0 ? '成功' : (row.status === 2 ? '执行中' : '失败') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" />
        <el-table-column prop="result" label="结果" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { listTasks, listRobots, listExecutions } from '@/api'

const stats = ref({ taskCount: 0, taskEnabled: 0, robotCount: 0, execCount: 0 })
const recent = ref<any[]>([])
const loading = ref(false)

const successRate = computed(() => {
  const slice = recent.value.slice(0, 100)
  if (!slice.length) return 0
  const ok = slice.filter(r => r.status === 0).length
  return ((ok / slice.length) * 100).toFixed(1)
})

async function load() {
  loading.value = true
  try {
    const [t, r, e] = await Promise.all([
      listTasks({ page: 1, size: 1 }),
      listRobots({ page: 1, size: 1 }),
      listExecutions({ page: 1, size: 10 })
    ])
    stats.value = {
      taskCount: t.data.data?.total ?? 0,
      taskEnabled: t.data.data?.records?.filter((x: any) => x.status === 1).length ?? 0,
      robotCount: r.data.data?.total ?? 0,
      execCount: e.data.data?.total ?? 0
    }
    recent.value = e.data.data?.records ?? []
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.stat-title { color: #909399; font-size: 13px; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; margin: 6px 0; }
.stat-sub { color: #c0c4cc; font-size: 12px; }
</style>
