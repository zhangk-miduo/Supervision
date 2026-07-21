import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.response.use(
  (response) => response.data,
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
export function listExecutions(params: { taskId?: number; status?: number; page?: number; size?: number }) {
  return http.get<ApiResult<Page<Execution>>>('/executions', { params })
}
