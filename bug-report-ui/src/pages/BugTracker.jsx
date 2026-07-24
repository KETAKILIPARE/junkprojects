import { useEffect, useState } from 'react'
import api from '../api'

const SEVERITY_COLORS = { LOW:'#22c55e', MEDIUM:'#f59e0b', HIGH:'#f97316', CRITICAL:'#ef4444' }
const STATUS_COLORS = { OPEN:'#3b82f6', IN_PROGRESS:'#f59e0b', RESOLVED:'#22c55e', CLOSED:'#64748b' }

export default function BugTracker({ onLogout }) {
  const [bugs, setBugs] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [rawDescription, setRawDescription] = useState('')
  const [loading, setLoading] = useState(false)
  const [filter, setFilter] = useState('ALL')

  const load = async () => {
    const params = filter !== 'ALL' ? { status: filter } : {}
    const res = await api.get('/bugs', { params })
    setBugs(res.data)
  }

  useEffect(() => { load() }, [filter])

  const submit = async e => {
    e.preventDefault()
    setLoading(true)
    try {
      await api.post('/bugs', { rawDescription })
      setRawDescription('')
      setShowForm(false)
      load()
    } finally { setLoading(false) }
  }

  const updateStatus = async (id, status) => {
    await api.patch(`/bugs/${id}/status`, { status })
    load()
  }

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>🐛 Bug Tracker</h1>
        <div style={{ display:'flex', gap:'1rem' }}>
          <button style={styles.btn} onClick={() => setShowForm(!showForm)}>+ Report Bug</button>
          <button style={{...styles.btn, background:'#475569'}} onClick={onLogout}>Logout</button>
        </div>
      </div>

      {showForm && (
        <form onSubmit={submit} style={styles.form}>
          <p style={styles.hint}>Describe the bug naturally — AI will structure it for you.</p>
          <textarea style={styles.textarea} rows={4}
            placeholder="e.g. When I click save on the profile page after changing my email, nothing happens and the old email stays. This only happens on Chrome."
            value={rawDescription} onChange={e => setRawDescription(e.target.value)} required />
          <button style={styles.btn} type="submit" disabled={loading}>
            {loading ? '🤖 AI is processing...' : 'Submit Bug'}
          </button>
        </form>
      )}

      <div style={styles.filters}>
        {['ALL','OPEN','IN_PROGRESS','RESOLVED','CLOSED'].map(s => (
          <button key={s} style={{...styles.filterBtn, background: filter===s ? '#3b82f6' : '#1e293b'}}
            onClick={() => setFilter(s)}>{s}</button>
        ))}
      </div>

      <div style={styles.list}>
        {bugs.map(bug => (
          <div key={bug.id} style={styles.card}>
            <div style={styles.cardTop}>
              <div style={{ display:'flex', gap:'0.5rem', alignItems:'center' }}>
                <span style={{...styles.badge, background: SEVERITY_COLORS[bug.severity] || '#64748b'}}>{bug.severity}</span>
                <span style={{...styles.badge, background: STATUS_COLORS[bug.status] || '#64748b'}}>{bug.status}</span>
              </div>
              <select style={styles.select} value={bug.status}
                onChange={e => updateStatus(bug.id, e.target.value)}>
                {['OPEN','IN_PROGRESS','RESOLVED','CLOSED'].map(s => <option key={s}>{s}</option>)}
              </select>
            </div>
            <p style={styles.rawDesc}>{bug.rawDescription}</p>
            {bug.stepsToReproduce && (
              <div style={styles.section}>
                <strong>Steps:</strong>
                <p style={styles.detail}>{bug.stepsToReproduce}</p>
              </div>
            )}
            {bug.expectedBehavior && (
              <div style={styles.section}>
                <strong>Expected:</strong> <span style={styles.detail}>{bug.expectedBehavior}</span>
              </div>
            )}
            {bug.actualBehavior && (
              <div style={styles.section}>
                <strong>Actual:</strong> <span style={styles.detail}>{bug.actualBehavior}</span>
              </div>
            )}
            {bug.suggestedLabels?.length > 0 && (
              <div style={styles.labels}>
                {bug.suggestedLabels.map(l => <span key={l} style={styles.label}>{l}</span>)}
              </div>
            )}
          </div>
        ))}
        {bugs.length === 0 && <p style={styles.empty}>No bugs found.</p>}
      </div>
    </div>
  )
}

const styles = {
  page: { minHeight:'100vh', background:'#0f172a', padding:'2rem', color:'#f1f5f9' },
  header: { display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'2rem' },
  title: { margin:0, fontSize:'1.75rem' },
  btn: { padding:'0.6rem 1.2rem', borderRadius:'8px', background:'#3b82f6', color:'#fff', border:'none', cursor:'pointer', fontSize:'0.95rem' },
  form: { background:'#1e293b', padding:'1.5rem', borderRadius:'12px', marginBottom:'2rem', display:'flex', flexDirection:'column', gap:'1rem' },
  hint: { color:'#94a3b8', margin:0, fontSize:'0.9rem' },
  textarea: { padding:'0.75rem', borderRadius:'8px', border:'1px solid #334155', background:'#0f172a', color:'#f1f5f9', fontSize:'0.95rem', resize:'vertical' },
  filters: { display:'flex', gap:'0.5rem', marginBottom:'1.5rem', flexWrap:'wrap' },
  filterBtn: { padding:'0.4rem 1rem', borderRadius:'999px', color:'#f1f5f9', border:'none', cursor:'pointer', fontSize:'0.85rem' },
  list: { display:'flex', flexDirection:'column', gap:'1rem' },
  card: { background:'#1e293b', borderRadius:'12px', padding:'1.5rem' },
  cardTop: { display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'0.75rem' },
  badge: { padding:'0.2rem 0.6rem', borderRadius:'999px', fontSize:'0.75rem', fontWeight:'bold', color:'#fff' },
  select: { padding:'0.4rem', borderRadius:'6px', background:'#0f172a', color:'#f1f5f9', border:'1px solid #334155', fontSize:'0.85rem' },
  rawDesc: { color:'#cbd5e1', marginBottom:'0.75rem' },
  section: { marginBottom:'0.5rem', fontSize:'0.9rem' },
  detail: { color:'#94a3b8', display:'inline' },
  labels: { display:'flex', gap:'0.5rem', flexWrap:'wrap', marginTop:'0.75rem' },
  label: { background:'#334155', padding:'0.2rem 0.6rem', borderRadius:'999px', fontSize:'0.75rem', color:'#94a3b8' },
  empty: { color:'#64748b', textAlign:'center', marginTop:'3rem' }
}
