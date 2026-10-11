import React, { useState } from 'react';
import api from '../api';
import { useAuth } from '../AuthContext';

export default function AuthPage({ initialMode = 'login', onBack }) {
  const { login } = useAuth();
  const [mode, setMode] = useState(initialMode); // 'login' | 'register'
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'PATIENT' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const endpoint = mode === 'login' ? '/api/auth/login' : '/api/auth/register';
      const payload = mode === 'login'
        ? { email: form.email, password: form.password }
        : form;
      const res = await api.post(endpoint, payload);
      login(res.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <div className="auth-logo">
          <h1><span style={{ color: '#5d7545' }}>Medi</span><span style={{ color: '#91ae6e' }}>Auth</span></h1>
          <p>Insurance Pre-Authorization Platform</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          {mode === 'register' && (
            <>
              <div className="form-group">
                <label>Full Name</label>
                <input name="name" value={form.name} onChange={handleChange}
                  placeholder="John Doe" required />
              </div>
              <div className="form-group">
                <label>Role</label>
                <select name="role" value={form.role} onChange={handleChange}>
                  <option value="PATIENT">Patient</option>
                  <option value="REVIEWER">Reviewer</option>
                </select>
              </div>
            </>
          )}

          <div className="form-group">
            <label>Email</label>
            <input name="email" type="email" value={form.email} onChange={handleChange}
              placeholder="you@example.com" required />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input name="password" type="password" value={form.password} onChange={handleChange}
              placeholder="••••••••" required />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Please wait…' : mode === 'login' ? 'Sign In' : 'Create Account'}
          </button>
        </form>

        <div className="auth-toggle">
          {mode === 'login' ? (
            <>Don't have an account? <button onClick={() => setMode('register')}>Register</button></>
          ) : (
            <>Already have an account? <button onClick={() => setMode('login')}>Sign In</button></>
          )}
        </div>

        {onBack && (
          <div className="auth-toggle">
            <button type="button" onClick={onBack}>Back to home</button>
          </div>
        )}

        {mode === 'login' && (
          <div style={{ marginTop: 20, padding: '12px 16px', background: '#f2f2f2', borderRadius: 8, fontSize: '.8rem', color: '#4b5563' }}>
            <strong>Demo accounts:</strong><br />
            Patient: alice@example.com / password123<br />
            Reviewer: reviewer@example.com / reviewer123
          </div>
        )}
      </div>
    </div>
  );
}
