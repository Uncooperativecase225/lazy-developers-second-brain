import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react';
import {
  Activity as ActivityIcon, ArrowUpRight, BrainCircuit, Check, CheckCircle2, CircleHelp,
  Clock3, Code2, Command, FileCode2, FolderGit2, GitBranch, History, LoaderCircle, MessageSquareText,
  Plus, RefreshCw, Search, ShieldCheck, Sparkles, Terminal, Trash2, X,
} from 'lucide-react';
import { api, type Activity, type Project, type Resume, type Session, type SystemStatus, type Task } from './lib/api';

type View = 'overview' | 'memory' | 'tasks' | 'ask';

function App() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [view, setView] = useState<View>('overview');
  const [status, setStatus] = useState<SystemStatus | null>(null);
  const [activity, setActivity] = useState<Activity | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [resume, setResume] = useState<Resume | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showAddProject, setShowAddProject] = useState(false);
  const [projectName, setProjectName] = useState('');
  const [repositoryPath, setRepositoryPath] = useState('');
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDescription, setTaskDescription] = useState('');
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const loadSequence = useRef(0);
  const activeIdRef = useRef<number | null>(activeId);
  activeIdRef.current = activeId;

  const activeProject = useMemo(() => projects.find((p) => p.id === activeId) ?? null, [projects, activeId]);
  const openTasks = tasks.filter((task) => task.status === 'OPEN' || task.status === 'IN_PROGRESS');

  const refreshProjects = useCallback(async (preferredId?: number) => {
    const data = await api.projects();
    setProjects(data);
    const nextId = preferredId ?? activeId;
    if (nextId && data.some((p) => p.id === nextId)) setActiveId(nextId);
    else setActiveId(data[0]?.id ?? null);
  }, [activeId]);

  const loadProjectData = useCallback(async (projectId: number) => {
    const sequence = ++loadSequence.current;
    setLoading(true); setError('');
    try {
      const [a, t, s] = await Promise.all([api.activity(projectId), api.tasks(projectId), api.sessions(projectId)]);
      if (sequence === loadSequence.current) { setActivity(a); setTasks(t); setSessions(s); }
    } catch (e) { if (sequence === loadSequence.current) setError(messageOf(e)); }
    finally { if (sequence === loadSequence.current) setLoading(false); }
  }, []);

  useEffect(() => {
    let active = true;
    void Promise.all([api.status(), api.projects()]).then(([system, data]) => {
      if (!active) return;
      setStatus(system); setProjects(data); setActiveId(data[0]?.id ?? null);
    }).catch((e: unknown) => { if (active) setError(messageOf(e)); });
    return () => { active = false; };
  }, []);

  useEffect(() => { if (activeId !== null) void loadProjectData(activeId); else { loadSequence.current += 1; setLoading(false); setActivity(null); setTasks([]); setSessions([]); } }, [activeId, loadProjectData]);

  async function addProject(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const created = await api.createProject(projectName.trim(), repositoryPath.trim());
      await refreshProjects(created.id); setResume(null); setAnswer(''); setShowAddProject(false); setProjectName(''); setRepositoryPath('');
      setNotice('Repository added. Its history is ready to inspect.');
    } catch (e) { setError(messageOf(e)); }
    finally { setBusy(false); }
  }

  async function resumeWork() {
    if (!activeProject) return;
    setBusy(true); setError(''); setNotice('');
    try {
      const result = await api.resume(activeProject.id);
      if (activeIdRef.current === activeProject.id) {
        setResume(result); setView('overview');
        await loadProjectData(activeProject.id);
        setNotice(result.aiGenerated ? 'Project context reconstructed and saved.' : 'Saved a fallback session. Check that Ollama is running to enable AI summaries.');
      }
    } catch (e) { setError(messageOf(e)); }
    finally { setBusy(false); }
  }

  async function addTask(event: FormEvent) {
    event.preventDefault(); if (!activeProject || !taskTitle.trim()) return;
    setBusy(true); setError('');
    try { await api.createTask(activeProject.id, taskTitle.trim(), taskDescription.trim()); setTaskTitle(''); setTaskDescription(''); if (activeIdRef.current === activeProject.id) { setTasks(await api.tasks(activeProject.id)); setNotice('Task saved.'); } }
    catch (e) { setError(messageOf(e)); }
    finally { setBusy(false); }
  }

  async function changeTaskStatus(task: Task, next: Task['status']) {
    if (!activeProject) return;
    try { const updated = await api.updateTask(activeProject.id, task.id, next); setTasks((current) => current.map((item) => item.id === task.id ? updated : item)); }
    catch (e) { setError(messageOf(e)); }
  }

  async function removeTask(task: Task) {
    if (!activeProject || !window.confirm(`Delete task “${task.title}”?`)) return;
    try { await api.deleteTask(activeProject.id, task.id); setTasks((current) => current.filter((item) => item.id !== task.id)); }
    catch (e) { setError(messageOf(e)); }
  }

  async function askQuestion(event: FormEvent) {
    event.preventDefault(); if (!activeProject || !question.trim()) return;
    setBusy(true); setError(''); setAnswer('');
    try { const result = await api.ask(activeProject.id, question.trim()); setAnswer(result.answer); }
    catch (e) { setError(messageOf(e)); }
    finally { setBusy(false); }
  }

  async function removeProject() {
    if (!activeProject || !window.confirm(`Remove “${activeProject.name}” from Second Brain? Your repository files will not be deleted, but saved project memory and tasks will be removed.`)) return;
    try { await api.deleteProject(activeProject.id); setResume(null); await refreshProjects(); setNotice('Project removed from the app. Repository files were not changed.'); }
    catch (e) { setError(messageOf(e)); }
  }

  const filteredSessions = sessions.filter((session) => `${session.summary} ${session.nextAction} ${session.branchName}`.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark"><BrainCircuit size={23} /></div><div><strong>second brain</strong><span>DEVELOPER MEMORY</span></div></div>
        <div className="workspace-label">WORKSPACE <button className="icon-button tiny" aria-label="Add project" onClick={() => setShowAddProject(true)}><Plus size={15} /></button></div>
        <div className="project-switcher">
          {projects.length === 0 ? <p className="muted small sidebar-empty">No projects yet. Add a Git repository to begin.</p> : projects.map((project) => (
            <button key={project.id} className={`project-option ${activeId === project.id ? 'selected' : ''}`} onClick={() => { setActiveId(project.id); setResume(null); setAnswer(''); setError(''); }}>
              <FolderGit2 size={17} /><span>{project.name}</span>{activeId === project.id && <span className="active-dot" />}
            </button>
          ))}
        </div>
        <div className="side-divider" />
        <div className="workspace-label">YOUR SPACE</div>
        <nav className="main-nav" aria-label="Main navigation">
          <button className={view === 'overview' ? 'active' : ''} onClick={() => setView('overview')}><Command size={17} /> Overview</button>
          <button className={view === 'memory' ? 'active' : ''} onClick={() => setView('memory')}><History size={17} /> Session memory <span className="nav-count">{sessions.length || ''}</span></button>
          <button className={view === 'tasks' ? 'active' : ''} onClick={() => setView('tasks')}><CheckCircle2 size={17} /> Tasks <span className="nav-count">{openTasks.length || ''}</span></button>
          <button className={view === 'ask' ? 'active' : ''} onClick={() => setView('ask')}><MessageSquareText size={17} /> Ask your project</button>
        </nav>
        <div className="sidebar-bottom">
          <div className="privacy-card"><ShieldCheck size={18} /><div><strong>Local by design</strong><p>Your code stays on this machine. AI runs through your local Ollama instance.</p></div></div>
          <div className="connection-row"><span className={`status-led ${status?.database === 'connected' ? 'good' : ''}`} />MySQL <span className="connection-value">{status?.database ?? 'checking'}</span></div>
          <div className="connection-row"><span className={`status-led ${status?.ollama === 'connected' ? 'good' : ''}`} />Ollama <span className="connection-value">{status?.ollama ?? 'checking'}</span></div>
          <div className="model-label"><Code2 size={13} /> {status?.model ?? 'qwen3.5:9b'}</div>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar"><div className="breadcrumbs"><span>Workspace</span><span className="crumb-slash">/</span><strong>{viewLabel(view)}</strong></div><div className="topbar-right"><span className="local-badge"><span className="status-led good" /> Local environment</span><button className="avatar" title="Local workspace">LB</button></div></header>
        <div className="content-wrap">
          {error && <div className="alert error-alert" role="alert"><CircleHelp size={17} /><span>{error}</span><button className="icon-button" aria-label="Dismiss error" onClick={() => setError('')}><X size={16} /></button></div>}
          {notice && <div className="alert success-alert" role="status"><Check size={17} /><span>{notice}</span><button className="icon-button" aria-label="Dismiss notification" onClick={() => setNotice('')}><X size={16} /></button></div>}
          {!activeProject ? <EmptyProjects onAdd={() => setShowAddProject(true)} /> : <>
            {view === 'overview' && <Overview project={activeProject} activity={activity} tasks={tasks} sessions={sessions} resume={resume} loading={loading} busy={busy} onResume={resumeWork} onTasks={() => setView('tasks')} onMemory={() => setView('memory')} onRefresh={() => { if (activeId !== null) void loadProjectData(activeId); }} onRemove={removeProject} />}
            {view === 'memory' && <MemoryView sessions={filteredSessions} searchTerm={searchTerm} onSearch={setSearchTerm} loading={loading} onResume={resumeWork} busy={busy} />}
            {view === 'tasks' && <TasksView tasks={tasks} title={taskTitle} description={taskDescription} onTitle={setTaskTitle} onDescription={setTaskDescription} onAdd={addTask} onStatus={changeTaskStatus} onDelete={removeTask} busy={busy} />}
            {view === 'ask' && <AskView question={question} answer={answer} onQuestion={setQuestion} onSubmit={askQuestion} busy={busy} projectName={activeProject.name} />}
          </>}
          <footer className="footer"><span>SECOND BRAIN <span className="footer-dot">·</span> LOCAL-FIRST DEVELOPER MEMORY</span><span>Built for uninterrupted flow <span className="footer-leaf">✳</span></span></footer>
        </div>
      </main>
      {showAddProject && <div className="modal-backdrop" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) setShowAddProject(false); }}><section className="modal" role="dialog" aria-modal="true" aria-labelledby="add-project-title"><div className="modal-heading"><div><span className="eyebrow">NEW WORKSPACE</span><h2 id="add-project-title">Add a repository</h2><p>Register a local Git repository. Files are never copied or uploaded.</p></div><button className="icon-button" aria-label="Close dialog" onClick={() => setShowAddProject(false)}><X size={18} /></button></div><form onSubmit={addProject} className="stack-form"><label>Project name<input autoFocus value={projectName} onChange={(e) => setProjectName(e.target.value)} placeholder="e.g. SettleSmart" maxLength={120} required /></label><label>Absolute repository path<input value={repositoryPath} onChange={(e) => setRepositoryPath(e.target.value)} placeholder="C:\Users\you\projects\my-app" maxLength={1024} required /><span className="field-hint">Use the full path to a folder that contains a .git repository.</span></label><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setShowAddProject(false)}>Cancel</button><button className="button primary" disabled={busy || !projectName.trim() || !repositoryPath.trim()}>{busy ? <LoaderCircle className="spin" size={16} /> : <Plus size={16} />} Add repository</button></div></form></section></div>}
    </div>
  );
}

function Overview({ project, activity, tasks, sessions, resume, loading, busy, onResume, onTasks, onMemory, onRefresh, onRemove }: { project: Project; activity: Activity | null; tasks: Task[]; sessions: Session[]; resume: Resume | null; loading: boolean; busy: boolean; onResume: () => void; onTasks: () => void; onMemory: () => void; onRefresh: () => void; onRemove: () => void }) {
  const open = tasks.filter((task) => task.status === 'OPEN' || task.status === 'IN_PROGRESS');
  const completed = tasks.filter((task) => task.status === 'DONE');
  return <>
    <section className="welcome-row"><div><div className="eyebrow"><span className="eyebrow-line" /> YOUR PROJECT CONTEXT</div><h1>Pick up where<br className="desktop-break" /> <em>you left off.</em></h1><p className="subtitle">Your code moves forward. Your memory shouldn't have to start over.</p></div><button className="button primary resume-button" onClick={onResume} disabled={busy}><Sparkles size={17} />{busy ? 'Thinking…' : 'Resume my work'}<ArrowUpRight size={16} /></button></section>
    <section className="project-banner"><div className="project-icon"><FolderGit2 size={22} /></div><div className="project-meta"><div className="project-title-row"><h2>{project.name}</h2><span className="tag"><span className="status-led good" /> Registered</span></div><p className="path-text" title={project.repositoryPath}>{project.repositoryPath}</p><div className="project-facts"><span><GitBranch size={14} /> {activity?.branch || project.defaultBranch || 'Branch unavailable'}</span><span><Clock3 size={14} /> Added {formatDate(project.createdAt)}</span></div></div><div className="project-commit"><span>LATEST COMMIT</span><code>{activity?.headCommit || '—'}</code></div><button className="icon-button danger-hover" title="Remove project from Second Brain" aria-label="Remove project" onClick={onRemove}><Trash2 size={15} /></button></section>
    <section className="stat-grid"><StatCard label="OPEN TASKS" value={String(open.length).padStart(2, '0')} note="Work still in progress" icon={<CheckCircle2 size={17} />} onClick={onTasks} /><StatCard label="SAVED SESSIONS" value={String(sessions.length).padStart(2, '0')} note="Context snapshots" icon={<History size={17} />} onClick={onMemory} /><StatCard label="COMPLETED TASKS" value={String(completed.length).padStart(2, '0')} note="Marked as done" icon={<Check size={17} />} onClick={onTasks} /><StatCard label="WORKTREE" value={activity?.workingTree?.startsWith('Working tree clean') ? 'Clean' : activity?.workingTree ? 'Changed' : '—'} note="Tracked file status" icon={<FileCode2 size={17} />} /> </section>
    <div className="section-heading"><div><span className="eyebrow">YOUR MOMENTUM</span><h2>Project memory</h2></div><button className="text-button" onClick={onMemory}>View session history <ArrowUpRight size={15} /></button></div>
    <div className="overview-grid"><section className="panel resume-panel"><div className="panel-top"><div className="panel-icon purple"><BrainCircuit size={18} /></div><div><h3>Last known state</h3><p>AI-generated project context</p></div><span className="live-pill">{resume ? 'JUST UPDATED' : 'READY'}</span></div>
      {resume ? <><p className="summary-text">{resume.summary}</p>{resume.completedTasks.length > 0 && <div className="task-snippet"><span className="snippet-label">RECENTLY COMPLETED</span>{resume.completedTasks.slice(0, 3).map((item, i) => <div className="snippet-row" key={`${item}-${i}`}><CheckCircle2 size={15} /><span>{item}</span></div>)}</div>}{resume.pendingTasks.length > 0 && <div className="task-snippet"><span className="snippet-label">STILL ON YOUR PLATE</span>{resume.pendingTasks.slice(0, 3).map((item, i) => <div className="snippet-row pending" key={`${item}-${i}`}><span className="tiny-ring" /><span>{item}</span></div>)}</div>}<div className="next-action"><span>NEXT SUGGESTED ACTION</span><p>{resume.nextAction}</p></div><p className="source-note">{resume.aiGenerated ? 'Generated locally with Qwen via Ollama.' : 'Fallback summary — local AI was unavailable.'}</p></> : <div className="empty-state compact"><div className="empty-illustration"><Sparkles size={21} /></div><h4>Your next session starts here</h4><p>Let Qwen inspect recent commits and working-tree changes to reconstruct what you were doing.</p><button className="button secondary" onClick={onResume} disabled={busy}><Sparkles size={15} /> Build my context</button></div>}
    </section>
    <section className="panel activity-panel"><div className="panel-top"><div className="panel-icon green"><ActivityIcon size={18} /></div><div><h3>Recent activity</h3><p>From your local Git history</p></div><button className="icon-button" title="Refresh activity" onClick={onRefresh}><RefreshCw size={15} /></button></div>
      {loading ? <div className="loading-line"><LoaderCircle className="spin" size={17} /> Reading repository…</div> : <><div className="activity-block"><span className="snippet-label">WORKING TREE</span><pre className="code-summary">{activity?.workingTree || 'No repository activity available.'}</pre></div><div className="activity-block"><span className="snippet-label">DIFF SUMMARY</span><pre className="code-summary">{activity?.diffStat || 'No diff summary available.'}</pre></div><div className="activity-block"><span className="snippet-label">CHANGED FILES</span><pre className="code-summary">{activity?.changedFiles || 'No changed files reported.'}</pre></div><div className="activity-foot"><Terminal size={14} /><span>Read-only Git inspection</span><span className="foot-spacer" /><span>Local repository</span></div></>}
    </section></div>
    <section className="tip-banner"><div className="tip-icon"><ShieldCheck size={18} /></div><div><strong>Your code stays yours.</strong><p>Repository analysis and Qwen inference run locally. Only summaries and tasks are stored in your MySQL database.</p></div><span className="tip-badge">LOCAL-FIRST</span></section>
  </>;
}

function StatCard({ label, value, note, icon, onClick }: { label: string; value: string; note: string; icon: ReactNode; onClick?: () => void }) {
  return <button className={`stat-card ${onClick ? 'clickable' : ''}`} onClick={onClick} disabled={!onClick}><div className="stat-card-top"><span>{label}</span><span className="stat-icon">{icon}</span></div><div className="stat-value">{value}</div><div className="stat-note">{note}</div></button>;
}

function MemoryView({ sessions, searchTerm, onSearch, loading, onResume, busy }: { sessions: Session[]; searchTerm: string; onSearch: (value: string) => void; loading: boolean; onResume: () => void; busy: boolean }) {
  return <><section className="page-heading"><div><div className="eyebrow"><span className="eyebrow-line" /> PERSISTENT CONTEXT</div><h1>Session <em>memory.</em></h1><p className="subtitle">A searchable trail of what changed, what you decided, and what comes next.</p></div><button className="button primary" onClick={onResume} disabled={busy}><Sparkles size={16} /> Capture current state</button></section>
    <div className="toolbar"><div className="search-box"><Search size={17} /><input value={searchTerm} onChange={(e) => onSearch(e.target.value)} placeholder="Search summaries, branches, next actions…" /></div><span className="results-count">{sessions.length} saved {sessions.length === 1 ? 'session' : 'sessions'}</span></div>
    {loading ? <LoadingState /> : sessions.length === 0 ? <EmptyState icon={<History size={23} />} title="No session memory yet" text="Capture your first project state to save a summary, completed work, pending tasks, and a suggested next action." /> : <div className="session-list">{sessions.map((session, index) => <article className="session-card" key={session.id}><div className="session-rail"><div className="session-dot">{index === 0 ? <Sparkles size={14} /> : <History size={14} />}</div><div className="session-line" /></div><div className="session-content"><div className="session-header"><div><span className="session-date">{formatDateTime(session.createdAt)}</span><h3>{session.summary}</h3></div><span className="session-branch"><GitBranch size={13} />{session.branchName || 'Unknown branch'}</span></div><div className="session-bottom"><div className="session-next"><span>NEXT STEP</span><p>{session.nextAction || 'No next action recorded.'}</p></div><code>{session.headCommit || 'no-commit'}</code></div>{session.pendingTasks.length > 0 && <div className="session-tags">{session.pendingTasks.slice(0, 4).map((task) => <span key={task} className="task-chip">{task}</span>)}</div>}</div></article>)}</div>}
  </>;
}

function TasksView({ tasks, title, description, onTitle, onDescription, onAdd, onStatus, onDelete, busy }: { tasks: Task[]; title: string; description: string; onTitle: (value: string) => void; onDescription: (value: string) => void; onAdd: (event: FormEvent) => void; onStatus: (task: Task, status: Task['status']) => void; onDelete: (task: Task) => void; busy: boolean }) {
  const [filter, setFilter] = useState<'ALL' | Task['status']>('ALL');
  const visible = tasks.filter((task) => filter === 'ALL' || task.status === filter);
  return <><section className="page-heading"><div><div className="eyebrow"><span className="eyebrow-line" /> ONE STEP AT A TIME</div><h1>Keep the <em>momentum.</em></h1><p className="subtitle">Small next actions beat a giant backlog. AI suggestions are drafts—verify before acting.</p></div></section>
    <section className="panel task-create-panel"><div className="panel-top"><div className="panel-icon purple"><Plus size={18} /></div><div><h3>Add a task</h3><p>Capture something you want to pick up next</p></div></div><form className="task-form" onSubmit={onAdd}><input value={title} onChange={(e) => onTitle(e.target.value)} placeholder="e.g. Add tests for expired sessions" maxLength={240} required /><input value={description} onChange={(e) => onDescription(e.target.value)} placeholder="Optional context or acceptance criteria" maxLength={2000} /><button className="button primary" disabled={busy || !title.trim()}><Plus size={16} /> Add task</button></form></section>
    <div className="toolbar task-toolbar"><div className="filter-tabs">{(['ALL', 'OPEN', 'IN_PROGRESS', 'DONE', 'DISMISSED'] as const).map((value) => <button key={value} className={filter === value ? 'selected' : ''} onClick={() => setFilter(value)}>{value === 'IN_PROGRESS' ? 'In progress' : value.charAt(0) + value.slice(1).toLowerCase()}</button>)}</div><span className="results-count">{visible.length} items</span></div>
    {visible.length === 0 ? <EmptyState icon={<CheckCircle2 size={23} />} title="Nothing on this list" text="Add a task or capture project context to get started." /> : <div className="task-list">{visible.map((task) => <article className={`task-row ${task.status === 'DONE' ? 'is-done' : ''}`} key={task.id}><button className={`task-check ${task.status === 'DONE' ? 'checked' : ''}`} aria-label={task.status === 'DONE' ? 'Mark task open' : 'Mark task done'} onClick={() => onStatus(task, task.status === 'DONE' ? 'OPEN' : 'DONE')}>{task.status === 'DONE' && <Check size={13} />}</button><div className="task-main"><strong>{task.title}</strong>{task.description && <p>{task.description}</p>}<div className="task-meta"><span className={`source-tag ${task.source === 'MANUAL' ? '' : 'ai-source'}`}>{task.source === 'MANUAL' ? 'MANUAL' : 'AI SUGGESTION'}</span><span>Added {formatDate(task.createdAt)}</span></div></div><select aria-label={`Status for ${task.title}`} value={task.status} onChange={(e) => onStatus(task, e.target.value as Task['status'])}><option value="OPEN">Open</option><option value="IN_PROGRESS">In progress</option><option value="DONE">Done</option><option value="DISMISSED">Dismissed</option></select><button className="icon-button danger-hover" aria-label={`Delete ${task.title}`} onClick={() => onDelete(task)}><Trash2 size={15} /></button></article>)}</div>}
  </>;
}

function AskView({ question, answer, onQuestion, onSubmit, busy, projectName }: { question: string; answer: string; onQuestion: (value: string) => void; onSubmit: (event: FormEvent) => void; busy: boolean; projectName: string }) {
  const suggestions = ['What was changed most recently?', 'What should I work on next?', 'Explain the current working-tree changes.'];
  return <><section className="page-heading"><div><div className="eyebrow"><span className="eyebrow-line" /> CONTEXT-AWARE ASSISTANT</div><h1>Ask your <em>project.</em></h1><p className="subtitle">Ask about {projectName}, its recent commits, working tree, and saved session memory.</p></div></section>
    <section className="ask-shell"><div className="ask-hero"><div className="ask-orb"><BrainCircuit size={29} /></div><span className="eyebrow">LOCAL MODEL · QWEN</span><h2>What do you want to remember?</h2><p>Answers are grounded in recent Git activity and saved context. The model can be wrong—verify important conclusions.</p></div><div className="suggestion-list">{suggestions.map((suggestion) => <button key={suggestion} onClick={() => onQuestion(suggestion)}><span>{suggestion}</span><ArrowUpRight size={15} /></button>)}</div><form className="ask-form" onSubmit={onSubmit}><textarea value={question} onChange={(e) => onQuestion(e.target.value)} placeholder="Ask something about this repository…" rows={3} maxLength={2000} required /><div className="ask-form-bottom"><span><ShieldCheck size={14} /> Context stays on your machine</span><button className="button primary" disabled={busy || !question.trim()}>{busy ? <LoaderCircle className="spin" size={16} /> : <Sparkles size={16} />} Ask Qwen</button></div></form>{answer && <div className="answer-card"><div className="answer-title"><div className="panel-icon purple"><MessageSquareText size={17} /></div><div><h3>Answer</h3><p>Generated by your local model</p></div></div><div className="answer-body">{answer}</div></div>}</section>
  </>;
}

function EmptyProjects({ onAdd }: { onAdd: () => void }) { return <div className="empty-project-page"><div className="empty-illustration large"><BrainCircuit size={34} /></div><span className="eyebrow">YOUR LOCAL DEVELOPER MEMORY</span><h1>Less remembering.<br /><em>More building.</em></h1><p>Add a Git repository and let your local AI summarize recent work, save session memory, and help you pick up where you left off.</p><button className="button primary" onClick={onAdd}><Plus size={17} /> Add your first repository</button><div className="empty-trust"><ShieldCheck size={15} /> Local repository · MySQL storage · Ollama inference</div></div>; }
function EmptyState({ icon, title, text }: { icon: ReactNode; title: string; text: string }) { return <div className="empty-state"><div className="empty-illustration">{icon}</div><h3>{title}</h3><p>{text}</p></div>; }
function LoadingState() { return <div className="loading-state"><LoaderCircle className="spin" size={21} /><span>Loading project context…</span></div>; }
function viewLabel(view: View) { return ({ overview: 'Overview', memory: 'Session memory', tasks: 'Tasks', ask: 'Ask your project' })[view]; }
function messageOf(error: unknown) { return error instanceof Error ? error.message : 'An unexpected error occurred.'; }
function formatDate(value: string) { if (!value) return 'recently'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' }).format(date); }
function formatDateTime(value: string) { if (!value) return 'Recently saved'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' }).format(date); }

export default App;
