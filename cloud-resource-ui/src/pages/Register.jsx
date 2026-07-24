import { useState } from 'react'
import api from '../api'

export default function Register({ onSwitch }) {
  const [form, setForm] = useState({ username: '', password: '', role: 'OPERATOR' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const submit = async e => {
    e.preventDefault()
    try {
      await api.post('/auth/register', form)
      setSuccess(true)
    } catch (err) {
      setError(err.response?.status === 409 ? 'Username already taken' : 'Registration failed')
    }
  }

  if (success) return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h2 style={{ color: '#22c55e', textAlign: 'center' }}>✅ Account created!</h2>
        <button style={styles.button} onClick={onSwitch}>Go to Login</button>
      </div>
    </div>
  )

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h2 style={styles.title}>☁️ Create Account</h2>
        <form onSubmit={submit} style={styles.form}>
          <input style={styles.input} placeholder="Username" value={form.username}
            onChange={e => setForm({ ...form, username: e.target.value })} required />
          <input style={styles.input} type="password" placeholder="Password" value={form.password}
            onChange={e => setForm({ ...form, password: e.target.value })} required />
          <select style={styles.input} value={form.role}
            onChange={e => setForm({ ...form, role: e.target.value })}>
            <option value="ADMIN">Admin</option>
            <option value="OPERATOR">Operator</option>
            <option value="VIEWER">Viewer</option>
          </select>
          {error && <p style={styles.error}>{error}</p>}
          <button style={styles.button} type="submit">Register</button>
          <button style={styles.link} type="button" onClick={onSwitch}>Already have an account? Login</button>
        </form>
      </div>
    </div>
  )
}

const styles = {
  container: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#0f172a' },
  card: { background: '#1e293b', padding: '2rem', borderRadius: '12px', width: '360px' },
  title: { color: '#f1f5f9', marginBottom: '1.5rem', textAlign: 'center' },
  form: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  input: { padding: '0.75rem', borderRadius: '8px', border: '1px solid #334155', background: '#0f172a', color: '#f1f5f9', fontSize: '1rem' },
  button: { padding: '0.75rem', borderRadius: '8px', background: '#3b82f6', color: '#fff', border: 'none', fontSize: '1rem', cursor: 'pointer' },
  link: { background: 'none', border: 'none', color: '#64748b', cursor: 'pointer', fontSize: '0.9rem' },
  error: { color: '#f87171', margin: 0 }
}
