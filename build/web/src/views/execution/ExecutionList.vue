<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.taskId" placeholder="任务ID" clearable />
      <el-select v-model="query.status" placeholder="执行状态" clearable @change="load"><el-option label="成功" value="SUCCESS"/><el-option label="部分成功" value="PARTIAL_SUCCESS"/><el-option label="失败" value="FAILED"/><el-option label="执行中" value="RUNNING"/></el-select>
      <el-select v-if="isAdmin" v-model="query.creatorAccountId" placeholder="创建账号" clearable filterable @change="load"><el-option v-for="a in accounts" :key="a.id" :label="`${a.username} · ${a.displayName}`" :value="a.id"/></el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="taskName" label="任务名称" min-width="150" />
      <el-table-column label="创建账号" min-width="160"><template #default="{row}"><div>{{row.creatorUsername||'未归属'}}</div><small>{{row.creatorDisplayName}}</small></template></el-table-column>
      <el-table-column label="触发" width="90"><template #default="{row}">{{row.triggerType==='MANUAL'?'手动':'定时'}}</template></el-table-column>
      <el-table-column label="状态" width="130"><template #default="{row}"><el-tag :type="tagType(row.status)">{{row.statusLabel}}</el-tag></template></el-table-column>
      <el-table-column prop="messageSummary" label="推送内容" min-width="220" show-overflow-tooltip />
      <el-table-column label="目标" width="90"><template #default="{row}">{{row.successCount}}/{{row.targetCount}}</template></el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="170" />
      <el-table-column label="耗时" width="100"><template #default="{row}">{{duration(row.durationMillis)}}</template></el-table-column>
      <el-table-column label="结果" min-width="180"><template #default="{row}">{{row.resultSummary}}</template></el-table-column>
      <el-table-column label="操作" width="90"><template #default="{row}"><el-button link @click="detail(row)">查看详情</el-button></template></el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev,pager,next,total" :total="total" :page-size="query.size" @current-change="page" />
    <el-drawer v-model="drawer" title="执行详情" size="620px"><template v-if="current"><el-descriptions border :column="1"><el-descriptions-item label="任务">{{current.summary.taskName}}</el-descriptions-item><el-descriptions-item label="创建账号">{{current.summary.creatorUsername||'未归属'}} · {{current.summary.creatorDisplayName}}</el-descriptions-item><el-descriptions-item label="状态">{{current.summary.statusLabel}}</el-descriptions-item><el-descriptions-item label="推送内容">{{current.summary.messageSummary}}</el-descriptions-item><el-descriptions-item label="调度判断">{{current.summary.scheduleDecisionReason||'满足执行条件'}}</el-descriptions-item><el-descriptions-item v-if="!current.summary.snapshotComplete" label="历史数据"><el-tag type="warning">历史记录未保存完整快照</el-tag></el-descriptions-item></el-descriptions><el-divider>目标群投递</el-divider><el-empty v-if="!current.deliveries?.length" description="本次执行没有目标群投递"/><el-timeline v-else><el-timeline-item v-for="d in current.deliveries" :key="d.id" :type="d.status==='SUCCESS'?'success':'danger'" :timestamp="d.sentAt||d.updatedAt"><b>{{d.groupNameSnapshot||'历史群'}} · {{d.pushNameSnapshot||'历史推送'}}</b><div>{{d.normalizedMessage||d.failureReason||'推送完成'}}</div><el-collapse v-if="d.technicalDetailRedacted"><el-collapse-item title="技术详情"><pre>{{d.technicalDetailRedacted}}</pre></el-collapse-item></el-collapse></el-timeline-item></el-timeline></template></el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import{onMounted,reactive,ref}from'vue'
import{getExecution,listExecutions,listAccounts}from'@/api'
const user=JSON.parse(localStorage.getItem('supervision_user')||'{}'),isAdmin=user.roles?.includes('ADMIN')===true
const rows=ref<any[]>([]),accounts=ref<any[]>([]),total=ref(0),loading=ref(false),drawer=ref(false),current=ref<any>(null)
const query=reactive<{taskId:string;status:string;creatorAccountId?:number;page:number;size:number}>({taskId:'',status:'',page:1,size:10})
async function load(){loading.value=true;try{const r=await listExecutions({taskId:query.taskId?Number(query.taskId):undefined,status:query.status||undefined,creatorAccountId:query.creatorAccountId,page:query.page,size:query.size});rows.value=r.data.data?.records??[];total.value=r.data.data?.total??0}finally{loading.value=false}}
function page(value:number){query.page=value;load()}
async function detail(row:any){const r=await getExecution(row.id);current.value=r.data.data;drawer.value=true}
function tagType(s:string){return s==='SUCCESS'?'success':s==='RUNNING'?'warning':s==='PARTIAL_SUCCESS'?'warning':'danger'}
function duration(ms:number|null){if(ms==null)return'-';return ms<1000?ms+'ms':(ms/1000).toFixed(1)+'s'}
onMounted(async()=>{if(isAdmin){const r=await listAccounts({page:1,size:100});accounts.value=r.data.data.records}load()})
</script>

<style scoped>.toolbar{display:flex;gap:10px;margin-bottom:16px}.toolbar .el-input{width:150px}.toolbar .el-select{width:190px}.pager{margin-top:14px;justify-content:flex-end}small{color:#909399}pre{white-space:pre-wrap}</style>
