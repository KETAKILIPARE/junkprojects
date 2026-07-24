import { useEffect, useState } from 'react'
import api from '../api'

const STATUSES = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE']
const STATUS_COLORS = { TODO: '#64748b', IN_PROGRESS: '#3b82f6', REVIEW: '#f59e0b', DONE: '#22c55e' }
const ROLE_COLORS = { ADMIN: '#3b82f6', MEMBER: '#64748b' }

function getUsername() {
  const token = localStorage.getItem('wf_token')
  if (!token) return null
  try { return JSON.parse(atob(token.split('.')[1])).sub } catch { return null }
}

export default function Board({ workspaceId, onBack }) {
  const [tasks, setTasks] = useState([])
  const [members, setMembers] = useState([])
  const [orgUsers, setOrgUsers] = useState([])
  const [tab, setTab] = useState('board')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ title: '', description: '', assignee: '' })
  const [inviteUsername, setInviteUsername] = useState('')
  const [inviteRole, setInviteRole] = useState('MEMBER')
  const [myRole, setMyRole] = useState(null)
  const [error, setError] = useState('')
  const currentUser = getUsername()

  const loadTasks = async () => {
    try {
      const res = await api.get(`/workspaces/${workspaceId}/tasks`)
      setTasks(res.data)
    } catch { setError('Failed to load tasks') }
  }

  const loadMembers = async () => {
    try {
      const res = await api.get(`/workspaces/${workspaceId}/members`)
      setMembers(res.data)
      const mine = res.data.find(m => m.username === currentUser)
      setMyRole(mine?.role || 'MEMBER')
    } catch { setError('Failed to load members') }
  }

  const loadOrgUsers = async () => {
    try {
      const res = await api.get('/users')
      setOrgUsers(res.data)
    } catch {}
  }

  useEffect(() => { loadTasks(); loadMembers(); loadOrgUsers() }, [workspaceId])

  const createTask = async e => {
    e.preventDefault()
    try {
      await api.post(`/workspaces/${workspaceId}/tasks`, form)
      setShowForm(false)
      setForm({ title: '', description: '', assignee: '' })
      loadTasks()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create task')
    }
  }

  const updateStatus = async (taskId, status) => {
    try { await api.patch(`/workspaces/${workspaceId}/tasks/${taskId}/status`, { status }); loadTasks() }
    catch (err) { setError(err.response?.data?.message || 'Failed to update status') }
  }

  const deleteTask = async taskId => {
    try { await api.delete(`/workspaces/${workspaceId}/tasks/${taskId}`); loadTasks() }
    catch { setError('Only ADMINs can delete tasks') }
  }

  const inviteMember = async e => {
    e.preventDefault()
    try {
      await api.post(`/workspaces/${workspaceId}/members`, { username: inviteUsername, role: inviteRole })
      setInviteUsername('')
      setInviteRole('MEMBER')
      loadMembers()
      loadOrgUsers()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add member')
    }
  }

  const removeMember = async username => {
    try { await api.delete(`/workspaces/${workspaceId}/members/${username}`); loadMembers(); loadOrgUsers() }
    catch (err) { setError(err.response?.data?.message || 'Failed to remove member') }
  }

  const byStatus = status => tasks.filter(t => t.status === status)
  const isAdmin = myRole === 'ADMIN'
  const memberUsernames = members.map(m => m.username)
  const nonMembers = orgUsers.filter(u => !memberUsernames.includes(u))

  return (
    <div style={styles.page}>
      {/* Top bar */}
      <div style={styles.topBar}>
        <button style={styles.backBtn} onClick={onBack}>← Back</button>
        <div style={styles.tabs}>
          <button style={{ ...styles.tab, borderBottom: tab === 'board' ? '2px solid #3b82f6' : 'none' }}
            onClick={() => setTab('board')}>📋 Board</button>
          <button style={{ ...styles.tab, borderBottom: tab === 'members' ? '2px solid #3b82f6' : 'none' }}
            onClick={() => setTab('members')}>👥 Members</button>
        </div>
        <div style={styles.userInfo}>
          <span style={styles.username}>👤 {currentUser}</span>
          {myRole && <span style={{ ...styles.roleBadge, background: ROLE_COLORS[myRole] }}>{myRole}</span>}
        </div>
      </div>

      {error && <p style={styles.error} onClick={() => setError('')}>{error} ✕</p>}

      {/* Board tab */}
      {tab === 'board' && (
        <>
          <div style={styles.boardHeader}>
            <h2 style={styles.boardTitle}>Task Board</h2>
            <button style={styles.btn} onClick={() => setShowForm(!showForm)}>+ New Task</button>
          </div>

          {showForm && (
            <form onSubmit={createTask} style={styles.form}>
              <input style={styles.input} placeholder="Task title" value={form.title}
                onChange={e => setForm({ ...form, title: e.target.value })} required />
              <input style={styles.input} placeholder="Description (optional)" value={form.description}
                onChange={e => setForm({ ...form, description: e.target.value })} />
              <select style={styles.input} value={form.assignee}
                onChange={e => setForm({ ...form, assignee: e.target.value })}>
                <option value="">Unassigned</option>
                {members.map(m => (
                  <option key={m.username} value={m.username}>{m.username}</option>
                ))}
              </select>
              <button style={styles.btn} type="submit">Add Task</button>
              <button style={styles.cancelBtn} type="button" onClick={() => setShowForm(false)}>Cancel</button>
            </form>
          )}

          <div style={styles.board}>
            {STATUSES.map(status => (
              <div key={status} style={styles.column}>
                <div style={{ ...styles.colHeader, borderColor: STATUS_COLORS[status] }}>
                  <span>{status.replace('_', ' ')}</span>
                  <span style={styles.count}>{byStatus(status).length}</span>
                </div>
                {byStatus(status).map(task => (
                  <div key={task.id} style={styles.card}>
                    <div style={styles.cardTop}>
                      <p style={styles.taskTitle}>{task.title}</p>
                      {isAdmin && (
                        <button style={styles.deleteBtn} onClick={() => deleteTask(task.id)}>✕</button>
                      )}
                    </div>
                    {task.description && <p style={styles.desc}>{task.description}</p>}
                    {task.assignee && (
                      <p style={styles.assignee}>👤 {task.assignee}</p>
                    )}
                    <select style={styles.statusSelect} value={task.status}
                      onChange={e => updateStatus(task.id, e.target.value)}>
                      {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                    </select>
                  </div>
                ))}
                {byStatus(status).length === 0 && <p style={styles.empty}>Empty</p>}
              </div>
            ))}
          </div>
        </>
      )}

      {/* Members tab */}
      {tab === 'members' && (
        <div style={styles.membersPage}>
          <h2 style={styles.boardTitle}>Members</h2>

          {isAdmin && nonMembers.length > 0 && (
            <form onSubmit={inviteMember} style={styles.form}>
              <select style={styles.input} value={inviteUsername}
                onChange={e => setInviteUsername(e.target.value)} required>
                <option value="">Select user to invite...</option>
                {nonMembers.map(u => (
                  <option key={u} value={u}>{u}</option>
                ))}
              </select>
              <select style={styles.input} value={inviteRole}
                onChange={e => setInviteRole(e.target.value)}>
                <option value="MEMBER">Member</option>
                <option value="ADMIN">Admin</option>
              </select>
              <button style={styles.btn} type="submit">Invite</button>
            </form>
          )}

          {isAdmin && nonMembers.length === 0 && (
            <p style={styles.hint}>All organisation users are already members.</p>
          )}

          <div style={styles.memberList}>
            {members.map(m => (
              <div key={m.username} style={styles.memberCard}>
                <div style={styles.memberInfo}>
                  <span style={styles.memberName}>
                    👤 {m.username}
                    {m.username === currentUser && <span style={styles.you}> (you)</span>}
                  </span>
                  <span style={{ ...styles.roleBadge, background: ROLE_COLORS[m.role] }}>{m.role}</span>
                </div>
                {isAdmin && m.username !== currentUser && (
                  <button style={styles.removeBtn} onClick={() => removeMember(m.username)}>Remove</button>
                )}
              </div>
            ))}
          </div>

          {!isAdmin && (
            <p style={styles.hint}>Only ADMINs can invite or remove members.</p>
          )}
        </div>
      )}
    </div>
  )
}

const styles = {
  page: { minHeight: '100vh', background: '#0f172a', padding: '2rem', color: '#f1f5f9' },
  topBar: { display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem', flexWrap: 'wrap' },
  backBtn: { padding: '0.5rem 1rem', borderRadius: '8px', background: '#475569', color: '#fff', border: 'none', cursor: 'pointer' },
  tabs: { display: 'flex', gap: '0.25rem', flex: 1 },
  tab: { padding: '0.6rem 1.2rem', background: 'none', color: '#f1f5f9', border: 'none', cursor: 'pointer', fontSize: '1rem' },
  userInfo: { display: 'flex', alignItems: 'center', gap: '0.5rem' },
  username: { color: '#94a3b8', fontSize: '0.95rem' },
  roleBadge: { padding: '0.2rem 0.6rem', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 'bold', color: '#fff' },
  boardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  boardTitle: { margin: 0, fontSize: '1.4rem' },
  btn: { padding: '0.6rem 1.2rem', borderRadius: '8px', background: '#3b82f6', color: '#fff', border: 'none', cursor: 'pointer' },
  cancelBtn: { padding: '0.6rem 1.2rem', borderRadius: '8px', background: '#475569', color: '#fff', border: 'none', cursor: 'pointer' },
  form: { display: 'flex', gap: '1rem', flexWrap: 'wrap', background: '#1e293b', padding: '1.5rem', borderRadius: '12px', marginBottom: '2rem', alignItems: 'center' },
  input: { padding: '0.6rem', borderRadius: '8px', border: '1px solid #334155', background: '#0f172a', color: '#f1f5f9', fontSize: '0.95rem' },
  board: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem' },
  column: { background: '#1e293b', borderRadius: '12px', padding: '1rem', minHeight: '400px' },
  colHeader: { fontWeight: 'bold', borderLeft: '4px solid', paddingLeft: '0.75rem', marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  count: { background: '#334155', borderRadius: '999px', padding: '0.1rem 0.5rem', fontSize: '0.8rem' },
  card: { background: '#0f172a', borderRadius: '8px', padding: '1rem', marginBottom: '0.75rem' },
  cardTop: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.25rem' },
  taskTitle: { fontWeight: 'bold', margin: 0, flex: 1, fontSize: '0.95rem' },
  deleteBtn: { background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: '1rem', padding: '0 0.25rem', lineHeight: 1 },
  desc: { color: '#94a3b8', fontSize: '0.82rem', margin: '0.25rem 0' },
  assignee: { color: '#64748b', fontSize: '0.8rem', margin: '0.25rem 0 0.5rem' },
  statusSelect: { width: '100%', padding: '0.35rem', borderRadius: '6px', background: '#1e293b', color: '#f1f5f9', border: '1px solid #334155', fontSize: '0.82rem', marginTop: '0.5rem' },
  empty: { color: '#334155', textAlign: 'center', marginTop: '2rem', fontSize: '0.9rem' },
  membersPage: { maxWidth: '600px' },
  memberList: { display: 'flex', flexDirection: 'column', gap: '0.75rem' },
  memberCard: { background: '#1e293b', borderRadius: '10px', padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  memberInfo: { display: 'flex', alignItems: 'center', gap: '0.75rem' },
  memberName: { fontSize: '0.95rem' },
  you: { color: '#64748b', fontSize: '0.85rem' },
  removeBtn: { padding: '0.3rem 0.8rem', borderRadius: '6px', background: '#ef4444', color: '#fff', border: 'none', cursor: 'pointer', fontSize: '0.85rem' },
  hint: { color: '#475569', marginTop: '1rem', fontSize: '0.9rem' },
  error: { background: '#450a0a', color: '#f87171', padding: '0.75rem 1rem', borderRadius: '8px', marginBottom: '1rem', cursor: 'pointer' }
}
