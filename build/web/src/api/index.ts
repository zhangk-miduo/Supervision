import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('supervision_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
)

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface Page<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface Task {
  id: number
  name: string
  description?: string
  status: number
  scheduleType: number
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface TaskNode {
  id?: number
  taskId?: number
  nodeType: string
  nodeOrder: number
  config: string
}

export interface TaskSchedule {
  id?: number
  taskId?: number
  cronExpression: string
  status: number
}

export interface TaskDetail {
  task: Task
  nodes: TaskNode[]
  schedule?: TaskSchedule
}

export interface Robot {
  id: number
  robotId: string
  name: string
  webhookUrl: string
  template?: string
  createdAt?: string
}

export interface Execution {
  id: number
  taskId: number
  status: number
  result?: string
  startTime: string
  endTime?: string
}

export interface NodeInput {
  nodeType: string
  nodeOrder: number
  config: string
}

export interface TaskCreateRequest {
  name: string
  description?: string
  scheduleType: number
  createdBy?: string
  cronExpression?: string
  nodes: NodeInput[]
}

// ---------- 任务 ----------
export function listTasks(params: { name?: string; page?: number; size?: number }) {
  return http.get<ApiResult<Page<Task>>>('/tasks', { params })
}
export function getTask(id: number) {
  return http.get<ApiResult<TaskDetail>>('/tasks/' + id)
}
export function createTask(req: TaskCreateRequest) {
  return http.post<ApiResult<number>>('/tasks', req)
}
export function updateTask(id: number, req: TaskCreateRequest) {
  return http.put<ApiResult<null>>('/tasks/' + id, req)
}
export function deleteTask(id: number) {
  return http.delete<ApiResult<null>>('/tasks/' + id)
}
export function executeTask(id: number) {
  return http.post<ApiResult<number>>('/tasks/' + id + '/execute')
}

// ---------- 机器人 ----------
export function listRobots(params: { name?: string; page?: number; size?: number }) {
  return http.get<ApiResult<Page<Robot>>>('/robots', { params })
}
export function listSelectableRobots() {
  return http.get<ApiResult<Array<{ id: number; name: string }>>>('/robots/selectable')
}
export function getRobot(id: number) {
  return http.get<ApiResult<Robot>>('/robots/' + id)
}
export function createRobot(req: Partial<Robot>) {
  return http.post<ApiResult<number>>('/robots', req)
}
export function updateRobot(id: number, req: Partial<Robot>) {
  return http.put<ApiResult<null>>('/robots/' + id, req)
}
export function deleteRobot(id: number) {
  return http.delete<ApiResult<null>>('/robots/' + id)
}
export function testRobot(id: number) {
  return http.post<ApiResult<string>>('/robots/' + id + '/test')
}

// ---------- 执行日志 ----------
export function listExecutions(params: { taskId?: number; status?: string; page?: number; size?: number }) {
  return http.get<ApiResult<Page<any>>>('/executions', { params })
}
export function getExecution(id: number) {
  return http.get<ApiResult<any>>('/executions/' + id)
}

// ---------- 认证与账号 ----------
export interface LoginResponse {
  token: string
  accountId: number
  username: string
  displayName: string
  mustChangePassword: boolean
  roles: string[]
}
export interface Account {
  id: number
  username: string
  displayName: string
  personId?: number
  status: number
  mustChangePassword: number
  lastLoginAt?: string
}
export function login(username: string, password: string) {
  return http.post<ApiResult<LoginResponse>>('/auth/login', { username, password })
}
export function changePassword(currentPassword: string, newPassword: string) {
  return http.post<ApiResult<null>>('/auth/change-password', { currentPassword, newPassword })
}
export function logout() { return http.post<ApiResult<null>>('/auth/logout') }
export function listAccounts(params: { page?: number; size?: number }) {
  return http.get<ApiResult<Page<Account>>>('/accounts', { params })
}
export function createAccount(req: { username:string; displayName:string; temporaryPassword:string; personId?:number; status:number; roles:string[] }) {
  return http.post<ApiResult<number>>('/accounts', req)
}
export function updateAccount(id:number, req: Partial<Account> & { roles?:string[] }) {
  return http.put<ApiResult<null>>('/accounts/' + id, req)
}
export function resetAccountPassword(id:number, temporaryPassword:string) {
  return http.post<ApiResult<null>>('/accounts/' + id + '/reset-password', { temporaryPassword })
}
// ---------- 企微、组织、消息与调度 ----------
export const getWecomSettings=()=>http.get<ApiResult<any>>('/settings/wecom')
export const saveWecomSettings=(data:any)=>http.put<ApiResult<null>>('/settings/wecom',data)
export const verifyWecomSettings=()=>http.post<ApiResult<any>>('/settings/wecom/verify')
export const syncWecom=()=>http.post<ApiResult<any>>('/wecom/sync')
export const listSyncLogs=()=>http.get<ApiResult<any[]>>('/wecom/sync-logs')
export const listDepartments=()=>http.get<ApiResult<any[]>>('/departments/tree')
export const listPersons=(name='')=>http.get<ApiResult<any[]>>('/persons',{params:{name}})
export const previewSchedule=(data:any)=>http.post<ApiResult<string[]>>('/schedules/preview',data)
export const previewMessage=(data:any)=>http.post<ApiResult<any>>('/messages/preview',data)
export const testMessage=(data:any)=>http.post<ApiResult<any>>('/messages/test-send',data)
export const listDeliveries=()=>http.get<ApiResult<any[]>>('/messages/deliveries')
export const getWorkdayCalendar=()=>http.get<ApiResult<any[]>>('/schedules/workday-calendar')
export const getOrganizationDepartments=()=>http.get<ApiResult<any[]>>('/organization/departments')
export const getOrganizationDepartment=(id:number)=>http.get<ApiResult<any>>('/organization/departments/'+id)
export const getOrganizationPersons=(params:any)=>http.get<ApiResult<Page<any>>>('/organization/persons',{params})
export const getOrganizationPerson=(id:number)=>http.get<ApiResult<any>>('/organization/persons/'+id)