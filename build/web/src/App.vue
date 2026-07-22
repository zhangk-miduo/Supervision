<template>
  <router-view v-if="authPage" />
  <el-container v-else class="app-container">
    <el-aside width="220px" class="app-aside"><div class="brand"><span class="brand-logo">S</span><div><div class="brand-title">Supervision</div><div class="brand-sub">企业智能督办平台</div></div></div>
      <el-menu :default-active="route.path" router class="app-menu" background-color="#1f2d3d" text-color="#c0c4cc" active-text-color="#409eff">
        <el-menu-item index="/dashboard"><el-icon><Odometer/></el-icon><span>仪表盘</span></el-menu-item>
        <el-menu-item index="/task"><el-icon><Files/></el-icon><span>任务管理</span></el-menu-item>
        <el-menu-item index="/robot"><el-icon><ChatDotRound/></el-icon><span>机器人管理</span></el-menu-item>
        <el-menu-item index="/execution"><el-icon><List/></el-icon><span>执行日志</span></el-menu-item>
        <el-menu-item index="/organization"><el-icon><UserFilled/></el-icon><span>组织人员</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="/settings/wecom"><el-icon><Setting/></el-icon><span>企业微信设置</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="/accounts"><el-icon><User/></el-icon><span>账号管理</span></el-menu-item>
      </el-menu></el-aside>
    <el-container><el-header class="app-header"><span class="header-title">{{route.meta.title}}</span><div><span class="user-name">{{user?.displayName}}</span><el-button link @click="signOut">退出</el-button></div></el-header><el-main class="app-main"><router-view/></el-main></el-container>
  </el-container>
</template>
<script setup lang="ts">import{computed}from'vue';import{useRoute,useRouter}from'vue-router';import{logout}from'@/api';const route=useRoute(),router=useRouter();const authPage=computed(()=>route.path==='/login'||route.path==='/change-password');const user=computed(()=>{const raw=localStorage.getItem('supervision_user');return raw?JSON.parse(raw):null});const isAdmin=computed(()=>user.value?.roles?.includes('ADMIN'));async function signOut(){try{await logout()}finally{localStorage.removeItem('supervision_token');localStorage.removeItem('supervision_user');router.replace('/login')}}</script>
<style>html,body,#app{height:100%;margin:0}.app-container{height:100vh}.app-aside{background:#1f2d3d}.brand{display:flex;align-items:center;gap:10px;padding:18px 16px;color:#fff}.brand-logo{width:36px;height:36px;border-radius:8px;background:#409eff;color:#fff;font-weight:700;font-size:20px;display:flex;align-items:center;justify-content:center}.brand-title{font-size:16px;font-weight:600}.brand-sub{font-size:12px;color:#909399}.app-menu{border-right:none}.app-header{display:flex;align-items:center;justify-content:space-between;background:#fff;border-bottom:1px solid #ebeef5}.header-title{font-size:18px;font-weight:600}.user-name{margin-right:14px;color:#606266}.app-main{background:#f5f7fa;padding:20px}</style>