import React, { useState, useEffect, useCallback } from 'react';
import api from '../api';

const STATUS_OPTIONS = ['', 'SUBMITTED', 'AI_REVIEWED', 'APPROVED', 'REJECTED', 'NEEDS_HUMAN_REVIEW', 'SETTLED'];

export default function DashboardPage() {
  const [claims, setClaims] = useState([]);
  const [selected, setSelected] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchClaims = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const res = await api.get('/api/claims', { params });
      setClaims(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      setError('Failed to load claims');
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => { fetchClaims(); }, [fetchClaims]);

  const fetchDetail = async (id) => {
    setError('');
    try {
      const res = await api.get(`/api/claims/${id}`);
      setSelected(res.data);
    } catch {
      setError('Failed to load claim details');
    }
  };

  if (selected) {
    return <ClaimDetail claim={selected} onBack={() => setSelected(null)} />;
  }

  return (
    <div className="page container">
      <h2 className="page-title">My Claims</h2>

      <div className="card" style={{ padding: '16px 20px', marginBottom: 16 }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <label style={{ fontWeight: 500, fontSize: '.875rem', color: '#374151' }}>Filter by status:</label>
          <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            style={{ padding: '6px 10px', borderRadius: 6, border: '1.5px solid #d1d5db', fontSize: '.875rem' }}>
            {STATUS_OPTIONS.map(s => (
              <option key={s} value={s}>{s || 'All Statuses'}</option>
            ))}
          </select>
          <button className="btn btn-outline btn-sm" onClick={fetchClaims}>Refresh</button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <div className="spinner" />}

      {!loading && claims.length === 0 && (
        <div className="empty-state">
          <p>No claims found. Submit your first claim to get started.</p>
        </div>
      )}

      <ul className="claim-list">
        {claims.map(claim => (
          <li key={claim.id} className="claim-item" onClick={() => fetchDetail(claim.id)}>
            <div className="claim-item-header">
              <span className="claim-item-title">
                #{claim.id} — {claim.claimType} · {claim.diagnosis}
              </span>
              <StatusBadge status={claim.status} />
            </div>
            <div className="claim-item-meta">
              ${claim.estimatedAmount?.toLocaleString()} · {claim.procedure} · {formatDate(claim.submittedAt)}
            </div>
            {claim.aiDecision && (
              <div style={{ marginTop: 6, fontSize: '.8rem', color: '#6b7280' }}>
                🤖 AI: <strong>{claim.aiDecision}</strong>
                {claim.aiConfidence != null ? ` (${Math.round(claim.aiConfidence * 100)}% confidence)` : ''}
              </div>
            )}
          </li>
        ))}
      </ul>

      {totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-outline btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
          <span style={{ padding: '6px 12px', fontSize: '.875rem', color: '#374151' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button className="btn btn-outline btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
        </div>
      )}
    </div>
  );
}

function ClaimDetail({ claim, onBack }) {
  return (
    <div className="page container">
      <button className="btn btn-outline btn-sm" onClick={onBack} style={{ marginBottom: 20 }}>
        ← Back to Claims
      </button>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
          <h3 style={{ fontWeight: 700, fontSize: '1.1rem' }}>Claim #{claim.id}</h3>
          <StatusBadge status={claim.status} />
        </div>
        <div className="form-row" style={{ marginBottom: 16 }}>
          <Detail label="Patient" value={claim.patientName} />
          <Detail label="Policy" value={claim.policyNumber} />
          <Detail label="Claim Type" value={claim.claimType} />
          <Detail label="Estimated Amount" value={`$${claim.estimatedAmount?.toLocaleString()}`} />
          <Detail label="Diagnosis" value={claim.diagnosis} />
          <Detail label="Procedure" value={claim.procedure} />
          <Detail label="Submitted" value={formatDate(claim.submittedAt)} />
          {claim.decidedAt && <Detail label="Decided" value={formatDate(claim.decidedAt)} />}
        </div>

        {claim.aiRationale && (
          <div className="ai-panel">
            <div className="ai-panel-title">🤖 AI Decision</div>
            {claim.aiDecision && (
              <div style={{ marginBottom: 8 }}>
                <StatusBadge status={claim.aiDecision} />
              </div>
            )}
            <div className="ai-rationale">{claim.aiRationale}</div>
            {claim.aiConfidence != null && (
              <div className="ai-confidence">Confidence: {Math.round(claim.aiConfidence * 100)}%</div>
            )}
          </div>
        )}

        {claim.documents?.length > 0 && (
          <div style={{ marginTop: 20 }}>
            <div className="card-title">Documents</div>
            {claim.documents.map(doc => (
              <div key={doc.id} style={{ fontSize: '.875rem', padding: '6px 0', borderBottom: '1px solid #f3f4f6' }}>
                <strong>{doc.docType}</strong> — <a href={doc.fileUrl} target="_blank" rel="noreferrer">{doc.fileUrl}</a>
                <span style={{ color: '#9ca3af', marginLeft: 8 }}>{formatDate(doc.uploadedAt)}</span>
              </div>
            ))}
          </div>
        )}

        {claim.history?.length > 0 && (
          <div style={{ marginTop: 20 }}>
            <div className="card-title">Status History</div>
            <ul className="timeline">
              {claim.history.map(h => (
                <li key={h.id} className="timeline-item">
                  <div className="timeline-label">
                    {h.fromStatus ? `${h.fromStatus} → ` : ''}{h.toStatus}
                  </div>
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
