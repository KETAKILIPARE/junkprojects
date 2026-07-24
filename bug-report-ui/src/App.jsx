import { useState } from 'react'
import Login from './pages/Login'
import BugTracker from './pages/BugTracker'

export default function App() {
  const [authed, setAuthed] = useState(!!localStorage.getItem('br_token'))

  const logout = () => { localStorage.removeItem('br_token'); setAuthed(false) }

  return authed
    ? <BugTracker onLogout={logout} />
    : <Login onLogin={() => setAuthed(true)} />
}
