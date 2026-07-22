<template>
  <div class="auth-page"><el-card class="auth-card"><h2>Supervision</h2><p>企业智能督办平台</p>
    <el-form @keyup.enter="submit"><el-form-item><el-input v-model="form.username" placeholder="账号" /></el-form-item>
    <el-form-item><el-input v-model="form.password" type="password" show-password placeholder="密码" /></el-form-item>
    <el-button type="primary" :loading="loading" class="full" @click="submit">登录</el-button></el-form>
  </el-card></div>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'; import { useRouter } from 'vue-router'; import { ElMessage } from 'element-plus'; import { login } from '@/api'
const router=useRouter(),loading=ref(false);const form=reactive({username:'',password:''})
async function submit(){if(!form.username||!form.password)return ElMessage.warning('请输入账号和密码');loading.value=true;try{const r=await login(form.username,form.password);const d=r.data.data;localStorage.setItem('supervision_token',d.token);localStorage.setItem('supervision_user',JSON.stringify(d));await router.replace(d.mustChangePassword?'/change-password':'/dashboard')}catch(e:any){ElMessage.error(e?.response?.data?.message||'登录失败')}finally{loading.value=false}}
</script>
<style scoped>.auth-page{height:100vh;display:flex;align-items:center;justify-content:center;background:#f3f6fb}.auth-card{width:360px;text-align:center}.auth-card p{color:#909399;margin-bottom:24px}.full{width:100%}</style>