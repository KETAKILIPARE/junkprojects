import { useEffect, useState } from 'react'
import api from '../api'

const STATUS_COLORS = { PENDING:'#f59e0b', RUNNING:'#22c55e', STOPPED:'#94a3b8', TERMINATED:'#ef4444' }

export default function Dashboard({ onLogout }) {
  const [resources, setResources] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name:'', type:'EC2', region:'us-east-1' })
  const [error, setError] = useState('')

  const load = async () => {
    try {
      const res = await api.get('/resources')
      setResources(res.data)
    } catch { setError('Failed to load resources') }
  }

  useEffect(() => { load() }, [])

  const create = async e => {
    e.preventDefault()
    try {
      await api.post('/resources', form)
      setShowForm(false)
      setForm({ name:'', type:'EC2', region:'us-east-1' })
      load()
    } catch { setError('Failed to create resource') }
  }

  const terminate = async id => {
    try { await api.delete(`/resources/${id}`); load() }
    catch { setError('Failed to terminate') }
  }

  const updateStatus = async (id, status) => {
    try { await api.patch(`/resources/${id}/status`, { status }); load() }
    catch { setError('Failed to update status') }
  }

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>☁️ Cloud Resources</h1>
        <div style={{ display:'flex', gap:'1rem' }}>
          <button style={styles.btn} onClick={() => setShowForm(!showForm)}>+ New Resource</button>
          <button style={{...styles.btn, background:'#475569'}} onClick={onLogout}>Logout</button>
        </div>
      </div>

      {error && <p style={styles.error}>{error}</p>}

      {showForm && (
        <form onSubmit={create} style={styles.form}>
          <input style={styles.input} placeholder="Resource name" value={form.name}
            onChange={e => setForm({...form, name:e.target.value})} required />
          <select style={styles.input} value={form.type} onChange={e => setForm({...form, type:e.target.value})}>
            <option>EC2</option><option>RDS</option><option>S3</option><option>LAMBDA</option>
          </select>
          <input style={styles.input} placeholder="Region" value={form.region}
            onChange={e => setForm({...form, region:e.target.value})} />
          <button style={styles.btn} type="submit">Create</button>
        </form>
      )}

      <div style={styles.grid}>
        {resources.map(r => (
          <div key={r.id} style={styles.card}>
            <div style={styles.cardHeader}>
              <span style={styles.name}>{r.name}</span>
              <span style={{...styles.badge, background: STATUS_COLORS[r.status] || '#64748b'}}>{r.status}</span>
            </div>
            <p style={styles.meta}>{r.type} · {r.region}</p>
            <p style={styles.meta}>Owner: {r.createdBy}</p>
            {r.status !== 'TERMINATED' && (
              <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <select style={styles.select} value={r.status}
                  onChange={e => updateStatus(r.id, e.target.value)}>
                  <option value="PENDING">PENDING</option>
                  <option value="RUNNING">RUNNING</option>
                  <option value="STOPPED">STOPPED</option>
                  <option value="TERMINATED">TERMINATED</option>
                </select>
                <button style={styles.deleteBtn} onClick={() => terminate(r.id)}>Terminate</button>
              </div>
            )}
          </div>
        ))}
        {resources.length === 0 && <p style={styles.empty}>No resources yet. Create one above.</p>}
      </div>
    </div>
  )
}

const styles = {
  page: { minHeight:'100vh', background:'#0f172a', padding:'2rem', color:'#f1f5f9' },
  header: { display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'2rem' },
  title: { margin:0, fontSize:'1.75rem' },
  btn: { padding:'0.6rem 1.2rem', borderRadius:'8px', background:'#3b82f6', color:'#fff', border:'none', cursor:'pointer', fontSize:'0.95rem' },
  form: { display:'flex', gap:'1rem', flexWrap:'wrap', background:'#1e293b', padding:'1.5rem', borderRadius:'12px', marginBottom:'2rem' },
  input: { padding:'0.6rem', borderRadius:'8px', border:'1px solid #334155', background:'#0f172a', color:'#f1f5f9', fontSize:'0.95rem' },
  grid: { display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(280px, 1fr))', gap:'1.5rem' },
  card: { background:'#1e293b', borderRadius:'12px', padding:'1.5rem' },
  cardHeader: { display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'0.5rem' },
  name: { fontWeight:'bold', fontSize:'1.1rem' },
  badge: { padding:'0.25rem 0.75rem', borderRadius:'999px', fontSize:'0.8rem', fontWeight:'bold', color:'#fff' },
  meta: { color:'#94a3b8', margin:'0.25rem 0', fontSize:'0.9rem' },
  deleteBtn: { padding:'0.4rem 1rem', borderRadius:'8px', background:'#ef4444', color:'#fff', border:'none', cursor:'pointer' },
  select: { padding:'0.4rem', borderRadius:'6px', background:'#0f172a', color:'#f1f5f9', border:'1px solid #334155', fontSize:'0.85rem', flex:1 },
  error: { color:'#f87171', marginBottom:'1rem' },
  empty: { color:'#64748b', gridColumn:'1/-1' }
}
