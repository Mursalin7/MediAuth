import React, { useEffect, useMemo, useState } from 'react';
import api from '../api';

const CATEGORIES = [
  { value: 'INDIVIDUAL', label: 'Individual' },
  { value: 'FAMILY_FLOATER', label: 'Family Floater' },
  { value: 'SENIOR', label: 'Senior Citizen' },
];

const INITIAL_FORM = {
  category: '', planId: '', hospitalizationType: '', diagnosis: '', treatment: '', estimatedCost: '',
  admissionDate: '', policyStartDate: '', amountClaimedThisYear: '0', preExistingRelated: false, details: '',
};

export default function SubmitClaimPage({ onSubmitted }) {
  const [form, setForm] = useState(INITIAL_FORM);
  const [plans, setPlans] = useState([]);
  const [plansError, setPlansError] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    api.get('/api/plans')
      .then((res) => setPlans(res.data))
      .catch(() => setPlansError('Could not load plans. Please refresh or sign in again.'));
  }, []);

  const filteredPlans = useMemo(
    () => plans.filter((plan) => plan.category === form.category),
    [plans, form.category]
  );
  const selectedPlan = plans.find((plan) => String(plan.id) === String(form.planId));

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : value,
      ...(name === 'category' ? { planId: '' } : {}),
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (new Date(form.admissionDate) < new Date(form.policyStartDate)) {
      setError('Admission date cannot be before policy start date');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        planId: Number(form.planId),
        hospitalizationType: form.hospitalizationType,
        diagnosis: form.diagnosis,
        treatment: form.treatment,
        estimatedCost: Number(form.estimatedCost),
        admissionDate: form.admissionDate,
        policyStartDate: form.policyStartDate,
        amountClaimedThisYear: Number(form.amountClaimedThisYear),
        preExistingRelated: form.preExistingRelated,
        details: form.details,
      };
      const response = await api.post('/api/claims', payload);
      setSuccess(response.data);
      setForm(INITIAL_FORM);
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Failed to submit claim');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="page container">
        <div className="card" style={{ borderLeft: '4px solid #16a34a' }}>
          <div className="alert alert-success" style={{ marginBottom: 12 }}>Claim submitted successfully!</div>
          <p><strong>Claim ID:</strong> #{success.id}</p>
          <p><strong>Status:</strong> <StatusBadge status={success.status} /></p>
          <div className="ai-panel" style={{ marginTop: 0 }}>
            <div className="ai-panel-title">AI Assessment</div>
            <p><StatusBadge status={success.aiDecision || success.status} /></p>
            <div className="ai-rationale">{success.aiRationale || 'This claim was routed for human review.'}</div>
            {success.clausesCited?.length > 0 && <p><strong>Cited clauses:</strong> {success.clausesCited.join(', ')}</p>}
            <p><strong>Missing information:</strong> {success.missingInformation?.length ? success.missingInformation.join(', ') : 'None reported'}</p>
            {success.aiConfidence != null && <div className="ai-confidence">Confidence: {Math.round(success.aiConfidence * 100)}%</div>}
            <p><strong>Estimated payable amount:</strong> ₹{Number(success.payableAmount || 0).toLocaleString('en-IN')}</p>
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
            <button className="btn btn-outline" onClick={() => setSuccess(null)}>Submit Another Claim</button>
            {onSubmitted && <button className="btn btn-primary" onClick={onSubmitted}>Go to My Claims →</button>}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page container">
      <h2 className="page-title">Submit Pre-Authorization Claim</h2>
      <div className="card">
        <div className="card-title">Claim Details</div>
        {error && <div className="alert alert-error">{error}</div>}
        {plansError && <div className="alert alert-error">{plansError}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Insurance type</label>
            <select name="category" value={form.category} onChange={handleChange} required>
              <option value="">Select insurance type</option>
              {CATEGORIES.map((category) => <option key={category.value} value={category.value}>{category.label}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label>Plan</label>
            <select name="planId" value={form.planId} onChange={handleChange} disabled={!form.category || plans.length === 0} required>
              <option value="">{plans.length ? 'Select a plan' : 'Loading plans…'}</option>
              {filteredPlans.map((plan) => <option key={plan.id} value={plan.id}>{plan.planName}</option>)}
            </select>
          </div>
          {selectedPlan && (
            <div className="card" style={{ padding: 12, marginBottom: 16 }}>
              <strong>{selectedPlan.planName}</strong><br />
              Sum insured: ₹{Number(selectedPlan.sumInsured).toLocaleString('en-IN')}{selectedPlan.category === 'FAMILY_FLOATER' ? ' shared' : ''} · Co-pay: {selectedPlan.copayPercent}% · Restoration: {selectedPlan.restoration}
            </div>
          )}
          <div className="form-group">
            <label>Type of hospitalization</label>
            <select name="hospitalizationType" value={form.hospitalizationType} onChange={handleChange} required>
              <option value="">Select type</option>
              <option value="PLANNED">Planned</option>
              <option value="EMERGENCY">Emergency</option>
              <option value="DAY_CARE">Day-care</option>
            </select>
          </div>
          <div className="form-group"><label>Diagnosis</label><input name="diagnosis" value={form.diagnosis} onChange={handleChange} required /></div>
          <div className="form-group"><label>Treatment</label><input name="treatment" value={form.treatment} onChange={handleChange} required /></div>
          <div className="form-group"><label>Estimated cost (₹)</label><input name="estimatedCost" type="number" min="0.01" step="0.01" value={form.estimatedCost} onChange={handleChange} required /></div>
          <div className="form-group"><label>Admission date</label><input name="admissionDate" type="date" value={form.admissionDate} onChange={handleChange} required /></div>
          <div className="form-group"><label>Policy start date</label><input name="policyStartDate" type="date" value={form.policyStartDate} onChange={handleChange} required /></div>
          <div className="form-group"><label>Amount claimed this year (₹)</label><input name="amountClaimedThisYear" type="number" min="0" step="0.01" value={form.amountClaimedThisYear} onChange={handleChange} required /></div>
          <div className="form-group">
            <label className="checkbox-label">
              <input name="preExistingRelated" type="checkbox" checked={form.preExistingRelated} onChange={handleChange} />
              <span>Related to a pre-existing condition</span>
            </label>
          </div>
          <div className="form-group"><label>Additional details (optional)</label><textarea name="details" value={form.details} onChange={handleChange} /></div>
          <button type="submit" className="btn btn-primary" disabled={loading || plans.length === 0}>
            {loading ? 'Waking up the server, this can take up to a minute…' : 'Submit Claim'}
          </button>
        </form>
      </div>
    </div>
  );
}

function StatusBadge({ status }) {
  return <span className={`badge badge-${status}`}>{status?.replace(/_/g, ' ')}</span>;
}
