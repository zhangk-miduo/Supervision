<template>
  <el-container class="app-container">
    <el-aside width="220px" class="app-aside">
      <div class="brand">
        <span class="brand-logo">S</span>
        <div class="brand-text">
          <div class="brand-title">Supervision</div>
          <div class="brand-sub">企业智能督办平台</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="app-menu" background-color="#1f2d3d" text-color="#c0c4cc" active-text-color="#409eff">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/task">
          <el-icon><Files /></el-icon>
          <span>任务管理</span>
        </el-menu-item>
        <el-menu-item index="/robot">
          <el-icon><ChatDotRound /></el-icon>
          <span>机器人管理</span>
        </el-menu-item>
        <el-menu-item index="/execution">
          <el-icon><List /></el-icon>
          <span>执行日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <span class="header-title">{{ currentTitle }}</span>
        <span class="header-tip">让企业管理，从人工推动走向智能督办</span>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || 'Supervision')
</script>

<style>
html, body, #app { height: 100%; margin: 0; }
.app-container { height: 100vh; }
.app-aside { background: #1f2d3d; }
.brand { display: flex; align-items: center; gap: 10px; padding: 18px 16px; color: #fff; }
.brand-logo { width: 36px; height: 36px; border-radius: 8px; background: #409eff; color: #fff; font-weight: 700; font-size: 20px; display: flex; align-items: center; justify-content: center; }
.brand-title { font-size: 16px; font-weight: 600; }
.brand-sub { font-size: 12px; color: #909399; }
.app-menu { border-right: none; }
.app-header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #ebeef5; }
.header-title { font-size: 18px; font-weight: 600; }
.header-tip { font-size: 13px; color: #909399; }
.app-main { background: #f5f7fa; padding: 20px; }
</style>
