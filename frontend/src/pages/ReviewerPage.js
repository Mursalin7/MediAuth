import React, { useState, useEffect, useCallback } from 'react';
import api from '../api';

const FILTER_OPTIONS = [
  { value: 'NEEDS_HUMAN_REVIEW', label: 'Needs Review' },
  { value: 'AI_REVIEWED', label: 'AI Reviewed' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: '', label: 'All Claims' },
];

export default function ReviewerPage() {
  const [claims, setClaims] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('NEEDS_HUMAN_REVIEW');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchClaims = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const res = await api.get('/api/claims', { params });
      setClaims(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch {
      setError('Failed to load claims');
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => { fetchClaims(); }, [fetchClaims]);

  const openClaim = async (id) => {
    try {
      const res = await api.get(`/api/claims/${id}`);
      setSelected(res.data);
    } catch {
      setError('Failed to load claim');
    }
  };

  const closeDetail = () => {
    setSelected(null);
    fetchClaims();
  };

  if (selected) {
    return <ReviewDetail claim={selected} onClose={closeDetail} />;
  }

  return (
    <div className="page container">
      <h2 className="page-title">Reviewer Dashboard</h2>

      <div className="tabs">
        {FILTER_OPTIONS.map(opt => (
          <button key={opt.value}
            className={`tab-btn ${statusFilter === opt.value ? 'active' : ''}`}
            onClick={() => { setStatusFilter(opt.value); setPage(0); }}>
            {opt.label}
          </button>
        ))}
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <div className="spinner" />}

      {!loading && claims.length === 0 && (
        <div className="empty-state"><p>No claims match this filter.</p></div>
      )}

      <ul className="claim-list">
        {claims.map(claim => (
          <li key={claim.id} className="claim-item" onClick={() => openClaim(claim.id)}>
            <div className="claim-item-header">
              <span className="claim-item-title">
                #{claim.id} — {claim.patientName} · {claim.claimType}
              </span>
              <StatusBadge status={claim.status} />
            </div>
            <div className="claim-item-meta">
              {claim.diagnosis} · ${claim.estimatedAmount?.toLocaleString()} · {formatDate(claim.submittedAt)}
            </div>
            {claim.aiDecision && (
              <div style={{ marginTop: 6, fontSize: '.8rem', color: '#5b21b6' }}>
                🤖 AI suggested: <strong>{claim.aiDecision}</strong>
                {claim.aiConfidence != null ? ` (${Math.round(claim.aiConfidence * 100)}% confidence)` : ''}
              </div>
            )}
          </li>
        ))}
      </ul>

      {totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-outline btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
          <span style={{ padding: '6px 12px', fontSize: '.875rem' }}>Page {page + 1} of {totalPages}</span>
          <button className="btn btn-outline btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
        </div>
      )}
    </div>
  );
}

function ReviewDetail({ claim, onClose }) {
  const [decision, setDecision] = useState('APPROVED');
  const [remarks, setRemarks] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const submit = async () => {
    if (!remarks.trim()) { setError('Remarks are required'); return; }
    setLoading(true);
    setError('');
    try {
      await api.patch(`/api/claims/${claim.id}/review`, { decision, remarks });
      setDone(true);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to submit review');
    } finally {
      setLoading(false);
    }
  };

  if (done) {
    return (
      <div className="page container">
        <div className="card" style={{ borderLeft: '4px solid #16a34a' }}>
          <div className="alert alert-success" style={{ marginBottom: 16 }}>
            ✅ Review submitted. Claim #{claim.id} has been {decision.toLowerCase()}.
          </div>
          <button className="btn btn-primary" onClick={onClose}>Back to Review Queue</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page container">
      <button className="btn btn-outline btn-sm" onClick={onClose} style={{ marginBottom: 20 }}>
        ← Back to Queue
      </button>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
          <h3 style={{ fontWeight: 700, fontSize: '1.1rem' }}>Claim #{claim.id} Review</h3>
          <StatusBadge status={claim.status} />
        </div>

        <div className="form-row" style={{ marginBottom: 16 }}>
          <Detail label="Patient" value={claim.patientName} />
          <Detail label="Policy" value={claim.policyNumber} />
          <Detail label="Claim Type" value={claim.claimType} />
          <Detail label="Amount" value={`$${claim.estimatedAmount?.toLocaleString()}`} />
          <Detail label="Diagnosis" value={claim.diagnosis} />
          <Detail label="Procedure" value={claim.procedure} />
        </div>

        {claim.aiRationale && (
          <div className="ai-panel" style={{ marginBottom: 20 }}>
            <div className="ai-panel-title">🤖 AI Assessment (for reference)</div>
            <div style={{ marginBottom: 8 }}>
              {claim.aiDecision && (
                <><span style={{ color: '#6b7280', fontSize: '.85rem' }}>Suggested: </span><StatusBadge status={claim.aiDecision} /></>
              )}
              {claim.aiConfidence != null && (
                <span className="ai-confidence" style={{ marginLeft: 10 }}>
                  Confidence: {Math.round(claim.aiConfidence * 100)}%
                </span>
              )}
            </div>
            <div className="ai-rationale">{claim.aiRationale}</div>
          </div>
        )}

        <div style={{ borderTop: '2px solid #e5e7eb', paddingTop: 20 }}>
          <div className="card-title">Your Decision</div>
          {error && <div className="alert alert-error">{error}</div>}

          <div className="form-group">
            <label>Decision</label>
            <select value={decision} onChange={(e) => setDecision(e.target.value)}>
              <option value="APPROVED">✅ Approve</option>
              <option value="REJECTED">❌ Reject</option>
              <option value="SETTLED">💳 Mark as Settled</option>
            </select>
          </div>

          <div className="form-group">
            <label>Reviewer Remarks</label>
            <textarea value={remarks} onChange={(e) => setRemarks(e.target.value)}
              rows={4} placeholder="Provide detailed reasoning for your decision…" />
          </div>

          <div className="review-actions">
            <button
              className={`btn ${decision === 'APPROVED' ? 'btn-success' : decision === 'REJECTED' ? 'btn-danger' : 'btn-primary'}`}
              onClick={submit} disabled={loading}>
              {loading ? 'Submitting…' : `Submit: ${decision}`}
            </button>
            <button className="btn btn-outline" onClick={onClose}>Cancel</button>
          </div>
        </div>

        {claim.history?.length > 0 && (
          <div style={{ marginTop: 20 }}>
            <div className="card-title">Claim History</div>
            <ul className="timeline">
              {claim.history.map(h => (
                <li key={h.id} className="timeline-item">
                  <div className="timeline-label">{h.fromStatus ? `${h.fromStatus} → ` : ''}{h.toStatus}</div>
                  <div className="timeline-actor">{h.actor} · {formatDate(h.timestamp)}</div>
                  {h.remarks && <div className="timeline-remarks">{h.remarks}</div>}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}

function Detail({ label, value }) {
  return (
    <div>
      <div style={{ fontSize: '.78rem', color: '#9ca3af', textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 2 }}>{label}</div>
      <div style={{ fontWeight: 500, fontSize: '.9rem' }}>{value || '—'}</div>
    </div>
  );
}

function StatusBadge({ status }) {
  return <span className={`badge badge-${status}`}>{status?.replace(/_/g, ' ')}</span>;
}

function formatDate(str) {
  if (!str) return '—';
  return new Date(str).toLocaleString();
}
