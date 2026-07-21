import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/dashboard/index.vue'
import TaskList from '@/views/task/TaskList.vue'
import RobotList from '@/views/robot/RobotList.vue'
import ExecutionList from '@/views/execution/ExecutionList.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: Dashboard, meta: { title: '仪表盘' } },
  { path: '/task', name: 'task', component: TaskList, meta: { title: '任务管理' } },
  { path: '/robot', name: 'robot', component: RobotList, meta: { title: '机器人管理' } },
  { path: '/execution', name: 'execution', component: ExecutionList, meta: { title: '执行日志' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
