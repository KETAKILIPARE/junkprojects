import { useState } from 'react'
import api from '../api'

export default function Login({ onLogin, onRegister }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')

  const submit = async e => {
    e.preventDefault()
    try {
      const res = await api.post('/auth/login', form)
      localStorage.setItem('token', res.data.token)
      onLogin()
    } catch {
      setError('Invalid credentials')
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h2 style={styles.title}>☁️ Cloud Resource Platform</h2>
        <form onSubmit={submit} style={styles.form}>
          <input style={styles.input} placeholder="Username" value={form.username}
            onChange={e => setForm({ ...form, username: e.target.value })} />
          <input style={styles.input} type="password" placeholder="Password" value={form.password}
            onChange={e => setForm({ ...form, password: e.target.value })} />
          {error && <p style={styles.error}>{error}</p>}
          <button style={styles.button} type="submit">Login</button>
          <button style={styles.link} type="button" onClick={onRegister}>No account? Register</button>
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
