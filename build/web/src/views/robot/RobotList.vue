<template>
  <el-card shadow="never">
    <el-tabs v-model="query.view" @tab-change="changeView">
      <el-tab-pane label="我创建的" name="owned" />
      <el-tab-pane label="公开可用" name="public" />
      <el-tab-pane v-if="isAdmin" label="全部" name="all" />
    </el-tabs>
    <div class="toolbar">
      <el-input v-model="query.name" placeholder="群名或消息推送名称" clearable @keyup.enter="load" />
      <el-select v-if="isAdmin&&query.view==='all'" v-model="query.creatorAccountId" placeholder="创建账号" clearable filterable @change="load">
        <el-option v-for="a in accounts" :key="a.id" :label="`${a.username} · ${a.displayName}`" :value="a.id" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="query.view==='owned'" type="success" @click="openCreate">新建消息推送</el-button>
    </div>
    <el-alert title="公开消息推送可被其他账号用于任务发送，但只有创建者能够修改配置、停用或执行配置测试。" type="info" :closable="false" />
    <el-table :data="rows" v-loading="loading" border stripe style="margin-top:14px">
      <el-table-column prop="groupName" label="群名" />
      <el-table-column prop="pushName" label="消息推送名称" />
      <el-table-column label="公开" width="90"><template #default="{row}"><el-tag :type="row.isPublic?'success':'info'">{{row.isPublic?'公开':'私有'}}</el-tag></template></el-table-column>
      <el-table-column label="创建账号" min-width="170"><template #default="{row}"><div>{{row.creatorUsername||'未归属'}}</div><small>{{row.creatorDisplayName}}</small></template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="row.pushStatus===1?'success':'info'">{{row.pushStatus===1?'启用':'停用'}}</el-tag></template></el-table-column>
      <el-table-column prop="webhookUrl" label="Webhook"><template #default="{row}">{{row.webhookUrl||'仅创建者可见'}}</template></el-table-column>
      <el-table-column prop="lastTestedAt" label="最后测试" width="170" />
      <el-table-column label="操作" width="190"><template #default="{row}"><template v-if="row.canEdit"><el-button link @click="openEdit(row)">编辑</el-button><el-button link type="warning" @click="test(row)">测试</el-button><el-button link type="danger" @click="disable(row)">停用</el-button></template><span v-else class="readonly">只读可用</span></template></el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev,pager,next,total" :total="total" :page-size="query.size" @current-change="page" />
    <el-dialog v-model="dialog" :title="form.id?'编辑消息推送':'新建消息推送'" width="580px">
      <el-form label-width="130px">
        <el-form-item label="群名" required><el-input v-model="form.groupName" placeholder="如：运营中心群" /></el-form-item>
        <el-form-item label="消息推送名称" required><el-input v-model="form.pushName" placeholder="如：日常督办推送" /></el-form-item>
        <el-form-item label="Webhook" required><el-input v-model="form.webhookUrl" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..." /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.enabled" active-text="启用" /></el-form-item>
        <el-form-item label="公开调用"><el-switch v-model="form.isPublic" active-text="所有账号可用于任务" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRobots, createRobot, updateRobot, deleteRobot, testRobot, getRobotUsageImpact, listAccounts } from '@/api'

const user=JSON.parse(localStorage.getItem('supervision_user')||'{}'),isAdmin=user.roles?.includes('ADMIN')===true
const rows=ref<any[]>([]),accounts=ref<any[]>([]),total=ref(0),loading=ref(false),dialog=ref(false),saving=ref(false)
const query=reactive<{name:string;view:'owned'|'public'|'all';creatorAccountId?:number;page:number;size:number}>({name:'',view:'owned',page:1,size:10})
const form=reactive<any>({id:0,groupName:'',pushName:'',webhookUrl:'',enabled:true,isPublic:false,originalPublic:false,remark:''})

async function load(){loading.value=true;try{const r=await listRobots(query);rows.value=r.data.data?.records??[];total.value=r.data.data?.total??0}finally{loading.value=false}}
function changeView(){query.page=1;query.creatorAccountId=undefined;load()}
function page(value:number){query.page=value;load()}
function openCreate(){Object.assign(form,{id:0,groupName:'',pushName:'',webhookUrl:'',enabled:true,isPublic:false,originalPublic:false,remark:''});dialog.value=true}
function openEdit(row:any){Object.assign(form,{id:row.id,groupName:row.groupName,pushName:row.pushName,webhookUrl:'******',enabled:row.pushStatus===1,isPublic:row.isPublic,originalPublic:row.isPublic,remark:''});dialog.value=true}
async function impact(id:number,action:string){const r=await getRobotUsageImpact(id),count=r.data.data.externalTaskCount||0;if(count>0)await ElMessageBox.confirm(`有 ${count} 个其他账号的启用任务正在使用该机器人，${action}后这些任务将停止发送。确认继续？`,'共享影响确认',{type:'warning'});return count}
async function save(){if(!form.groupName.trim()||!form.pushName.trim()||(!form.id&&!form.webhookUrl.trim()))return ElMessage.warning('请填写群名、消息推送名称和 Webhook');if(form.id&&form.originalPublic&&!form.isPublic)await impact(form.id,'改为私有');saving.value=true;try{const payload={groupName:form.groupName,pushName:form.pushName,webhookUrl:form.webhookUrl,status:form.enabled?1:0,isPublic:form.isPublic,remark:form.remark};form.id?await updateRobot(form.id,payload):await createRobot(payload);dialog.value=false;ElMessage.success('已保存');load()}finally{saving.value=false}}
async function test(row:any){await testRobot(row.id);ElMessage.success('测试发送成功');load()}
async function disable(row:any){await impact(row.id,'停用');await ElMessageBox.confirm('确认停用该消息推送？');await deleteRobot(row.id);ElMessage.success('已停用');load()}
onMounted(async()=>{if(isAdmin){const r=await listAccounts({page:1,size:100});accounts.value=r.data.data.records}load()})
</script>

<style scoped>.toolbar{display:flex;gap:10px;margin-bottom:14px}.toolbar .el-input{width:240px}.toolbar .el-select{width:220px}.pager{margin-top:14px;justify-content:flex-end}.readonly,small{color:#909399}</style>
