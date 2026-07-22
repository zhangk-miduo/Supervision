import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/dashboard/index.vue'
import TaskList from '@/views/task/TaskList.vue'
import RobotList from '@/views/robot/RobotList.vue'
import ExecutionList from '@/views/execution/ExecutionList.vue'
import Login from '@/views/auth/Login.vue'
import ChangePassword from '@/views/auth/ChangePassword.vue'
import AccountList from '@/views/account/AccountList.vue'
import PersonList from '@/views/organization/PersonList.vue'
import WecomSettings from '@/views/settings/WecomSettings.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'login', component: Login, meta: { public: true, title: '登录' } },
  { path: '/change-password', name: 'change-password', component: ChangePassword, meta: { title: '修改密码', passwordChange: true } },
  { path: '/dashboard', name: 'dashboard', component: Dashboard, meta: { title: '仪表盘' } },
  { path: '/task', name: 'task', component: TaskList, meta: { title: '任务管理' } },
  { path: '/robot', name: 'robot', component: RobotList, meta: { title: '机器人管理' } },
  { path: '/execution', name: 'execution', component: ExecutionList, meta: { title: '执行日志' } },
  { path: '/accounts', name: 'accounts', component: AccountList, meta: { title: '账号管理', admin: true } },
  { path: '/organization', name: 'organization', component: PersonList, meta: { title: '组织人员' } },
  { path: '/settings/wecom', name: 'wecom-settings', component: WecomSettings, meta: { title: '企业微信设置', admin: true } }
]
const router=createRouter({history:createWebHistory(),routes})
router.beforeEach((to)=>{const token=localStorage.getItem('supervision_token');const raw=localStorage.getItem('supervision_user');const user=raw?JSON.parse(raw):null;if(to.meta.public)return token?(user?.mustChangePassword?'/change-password':'/dashboard'):true;if(!token)return'/login';if(user?.mustChangePassword&&!to.meta.passwordChange)return'/change-password';if(to.meta.admin&&!user?.roles?.includes('ADMIN'))return'/dashboard';return true})
export default router