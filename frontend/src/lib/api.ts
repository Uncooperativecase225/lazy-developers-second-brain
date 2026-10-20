const API_BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) { super(message); this.name = 'ApiError'; this.status = status; }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...init?.headers },
    });
  } catch {
    throw new ApiError('Cannot reach the backend. Start Spring Boot and try again.', 0);
  }
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json() as { detail?: string; message?: string };
      message = body.detail || body.message || message;
    } catch { /* response may have no JSON body */ }
    throw new ApiError(message, response.status);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export interface Project { id: number; name: string; repositoryPath: string; defaultBranch: string | null; createdAt: string; lastAccessedAt: string; }
export interface Task { id: number; projectId: number; title: string; description: string | null; status: 'OPEN' | 'IN_PROGRESS' | 'DONE' | 'DISMISSED'; source: string; sourceReference: string | null; createdAt: string; updatedAt: string; }
export interface Session { id: number; projectId: number; summary: string; completedTasks: string[]; pendingTasks: string[]; nextAction: string; branchName: string; headCommit: string; createdAt: string; }
export interface Resume { projectName: string; branchName: string; headCommit: string; workingTreeSummary: string; summary: string; completedTasks: string[]; pendingTasks: string[]; nextAction: string; sessionId: number; aiGenerated: boolean; }
export interface Activity { branch: string; headCommit: string; recentCommits: string; workingTree: string; diffStat: string; changedFiles: string; }
export interface SystemStatus { application: string; database: string; ollama: string; model: string; mode: string; }

export const api = {
  projects: () => request<Project[]>('/projects'),
  project: (id: number) => request<Project>(`/projects/${id}`),
  createProject: (name: string, repositoryPath: string) => request<Project>('/projects', { method: 'POST', body: JSON.stringify({ name, repositoryPath }) }),
  deleteProject: (id: number) => request<void>(`/projects/${id}`, { method: 'DELETE' }),
  activity: (id: number) => request<Activity>(`/projects/${id}/activity`),
  resume: (id: number) => request<Resume>(`/projects/${id}/resume`, { method: 'POST', body: '{}' }),
  sessions: (id: number) => request<Session[]>(`/projects/${id}/sessions`),
  tasks: (id: number) => request<Task[]>(`/projects/${id}/tasks`),
  createTask: (id: number, title: string, description: string) => request<Task>(`/projects/${id}/tasks`, { method: 'POST', body: JSON.stringify({ title, description }) }),
  updateTask: (projectId: number, taskId: number, status: Task['status']) => request<Task>(`/projects/${projectId}/tasks/${taskId}`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  deleteTask: (projectId: number, taskId: number) => request<void>(`/projects/${projectId}/tasks/${taskId}`, { method: 'DELETE' }),
  ask: (id: number, question: string) => request<{ answer: string; aiGenerated: boolean }>(`/projects/${id}/ask`, { method: 'POST', body: JSON.stringify({ question }) }),
  status: () => request<SystemStatus>('/system/status'),
};
