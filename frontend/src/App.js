import React, { useState } from 'react';
import { AuthProvider, useAuth } from './AuthContext';
import AuthPage from './pages/AuthPage';
import LandingPage from './pages/LandingPage';
import DashboardPage from './pages/DashboardPage';
import SubmitClaimPage from './pages/SubmitClaimPage';
import ReviewerPage from './pages/ReviewerPage';

function AppShell() {
  const { user, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('dashboard');
  const [authMode, setAuthMode] = useState(null);
  const handleSignOut = () => {
    logout();
    setAuthMode(null);
  };

  if (!user) {
    if (authMode === null) {
      return (
        <LandingPage
          onGetStarted={() => setAuthMode('register')}
          onSignIn={() => setAuthMode('login')}
        />
      );
    }
    return <AuthPage initialMode={authMode} onBack={() => setAuthMode(null)} />;
  }

  const isReviewer = user.role === 'REVIEWER';

  const nav = isReviewer
    ? [{ id: 'review', label: 'Review Queue' }]
    : [
        { id: 'dashboard', label: 'My Claims' },
        { id: 'submit', label: '+ New Claim' },
      ];

  return (
    <>
      <nav className="navbar">
        <div className="navbar-brand">Medi<span>Auth</span></div>
        <div className="navbar-links">
          {nav.map(n => (
            <button key={n.id} onClick={() => setActiveTab(n.id)}
              style={activeTab === n.id ? { background: 'rgba(255,255,255,.3)' } : {}}>
              {n.label}
            </button>
          ))}
          <span className="navbar-user">{user.name} ({user.role})</span>
          <button onClick={handleSignOut}>Sign Out</button>
        </div>
      </nav>

      {isReviewer ? (
        <ReviewerPage />
      ) : (
        <>
          {activeTab === 'dashboard' && <DashboardPage />}
          {activeTab === 'submit' && (
            <SubmitClaimPage onSubmitted={() => setActiveTab('dashboard')} />
          )}
        </>
      )}
    </>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  );
}
