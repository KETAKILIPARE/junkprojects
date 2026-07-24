import { useEffect, useState } from 'react'
import api from '../api'

function getUsername() {
  const token = localStorage.getItem('wf_token')
  if (!token) return null
  try { return JSON.parse(atob(token.split('.')[1])).sub } catch { return null }
}

function getSystemRole() {
  const token = localStorage.getItem('wf_token')
  if (!token) return null
  try {
    const roles = JSON.parse(atob(token.split('.')[1])).roles || []
    return roles.includes('ROLE_SYSTEM_ADMIN') ? 'SYSTEM_ADMIN' : 'SYSTEM_MEMBER'
  } catch { return null }
}

export default function Workspaces({ onSelect, onLogout }) {
  const [workspaces, setWorkspaces] = useState([])
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const username = getUsername()
  const systemRole = getSystemRole()
  const isSystemAdmin = systemRole === 'SYSTEM_ADMIN'

  const load = async () => {
    const res = await api.get('/workspaces')
    setWorkspaces(res.data)
  }

  useEffect(() => { load() }, [])

  const create = async e => {
    e.preventDefault()
    try {
      await api.post('/workspaces', { name })
      setName('')
      load()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create workspace')
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>📋 Workflow Manager</h1>
        <div style={styles.userBar}>
          <span style={styles.userInfo}>
            👤 <strong>{username}</strong>
            <span style={{ ...styles.badge, background: isSystemAdmin ? '#3b82f6' : '#64748b' }}>
              {isSystemAdmin ? 'Admin' : 'Member'}
            </span>
          </span>
          <button style={styles.logoutBtn} onClick={onLogout}>Logout</button>
        </div>
      </div>

      {error && <p style={styles.error} onClick={() => setError('')}>{error} ✕</p>}

      {isSystemAdmin && (
        <form onSubmit={create} style={styles.form}>
          <input style={styles.input} placeholder="New workspace name" value={name}
            onChange={e => setName(e.target.value)} required />
          <button style={styles.btn} type="submit">+ Create Workspace</button>
        </form>
      )}

      {!isSystemAdmin && workspaces.length === 0 && (
        <div style={styles.emptyState}>
          <p style={styles.emptyTitle}>No workspaces yet</p>
          <p style={styles.emptyHint}>You'll appear here once an Admin invites you to a workspace.</p>
        </div>
      )}

      <div style={styles.grid}>
        {workspaces.map(w => (
          <div key={w.id} style={styles.card} onClick={() => onSelect(w.id)}>
            <h3 style={styles.wsName}>{w.name}</h3>
            <p style={styles.meta}>Click to open board →</p>
          </div>
        ))}
      </div>
    </div>
  )
}

const styles = {
  page: { minHeight: '100vh', background: '#0f172a', padding: '2rem', color: '#f1f5f9' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' },
  title: { margin: 0 },
  userBar: { display: 'flex', alignItems: 'center', gap: '1rem' },
  userInfo: { display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#94a3b8' },
  badge: { padding: '0.2rem 0.6rem', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 'bold', color: '#fff' },
  logoutBtn: { padding: '0.5rem 1rem', borderRadius: '8px', background: '#475569', color: '#fff', border: 'none', cursor: 'pointer' },
  form: { display: 'flex', gap: '1rem', marginBottom: '2rem' },
  input: { padding: '0.6rem', borderRadius: '8px', border: '1px solid #334155', background: '#1e293b', color: '#f1f5f9', fontSize: '0.95rem', flex: 1 },
  btn: { padding: '0.6rem 1.2rem', borderRadius: '8px', background: '#3b82f6', color: '#fff', border: 'none', cursor: 'pointer' },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '1.5rem' },
  card: { background: '#1e293b', borderRadius: '12px', padding: '1.5rem', cursor: 'pointer' },
  wsName: { margin: '0 0 0.5rem', fontSize: '1.2rem' },
  meta: { color: '#64748b', margin: 0, fontSize: '0.9rem' },
  emptyState: { textAlign: 'center', marginTop: '4rem' },
  emptyTitle: { color: '#f1f5f9', fontSize: '1.2rem', marginBottom: '0.5rem' },
  emptyHint: { color: '#64748b' },
  error: { background: '#450a0a', color: '#f87171', padding: '0.75rem 1rem', borderRadius: '8px', marginBottom: '1rem', cursor: 'pointer' }
}
