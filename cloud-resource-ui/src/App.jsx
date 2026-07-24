import { useState } from 'react'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'

export default function App() {
  const [screen, setScreen] = useState(localStorage.getItem('token') ? 'dashboard' : 'login')

  const logout = () => { localStorage.removeItem('token'); setScreen('login') }

  if (screen === 'dashboard') return <Dashboard onLogout={logout} />
  if (screen === 'register') return <Register onSwitch={() => setScreen('login')} />
  return <Login onLogin={() => setScreen('dashboard')} onRegister={() => setScreen('register')} />
}
