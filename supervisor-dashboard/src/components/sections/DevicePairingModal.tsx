import React, { useState } from 'react';
import { Smartphone, CheckCircle, AlertCircle, Clock, X, KeyRound } from 'lucide-react';
import { claimPairingSession, getPairingStatus, saveDashboardToken } from '../../services/relay';

interface DevicePairingModalProps {
  isOpen: boolean;
  onClose: () => void;
  onPairingComplete: () => void;
}

export const DevicePairingModal: React.FC<DevicePairingModalProps> = ({
  isOpen,
  onClose,
  onPairingComplete,
}) => {
  const [pairingCode, setPairingCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [claimedSessionId, setClaimedSessionId] = useState<string | null>(null);
  const [claimedDevice, setClaimedDevice] = useState<{ name: string; model: string } | null>(null);
  const [pollingStatus, setPollingStatus] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleClaim = async (e: React.FormEvent) => {
    e.preventDefault();
    if (pairingCode.trim().length < 4) {
      setError('Please enter the 6-character code shown on your Android phone');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const claim = await claimPairingSession(pairingCode);
      setClaimedSessionId(claim.session_id);
      setClaimedDevice({ name: claim.device_name, model: claim.device_model });
      setPollingStatus('WAITING_APPROVAL');

      // Poll for phone user approval
      const pollInterval = setInterval(async () => {
        try {
          const status = await getPairingStatus(claim.session_id);
          if (status.status === 'APPROVED' && status.token) {
            clearInterval(pollInterval);
            saveDashboardToken(status.token, status.device_id);
            setPollingStatus('APPROVED');
            setTimeout(() => {
              onPairingComplete();
              onClose();
            }, 1200);
          } else if (status.status === 'DENIED') {
            clearInterval(pollInterval);
            setPollingStatus('DENIED');
            setError('Connection claim was explicitly denied on the mobile device.');
            setLoading(false);
          } else if (status.status === 'EXPIRED') {
            clearInterval(pollInterval);
            setPollingStatus('EXPIRED');
            setError('Pairing session expired. Please generate a new code on your phone.');
            setLoading(false);
          }
        } catch (_) {}
      }, 1500);
    } catch (err: any) {
      setError(err.message || 'Failed to claim pairing session. Verify pairing code.');
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(6px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '20px',
      }}
    >
      <div
        style={{
          backgroundColor: 'var(--surface-dark, #121824)',
          border: '1px solid var(--border-color, rgba(255, 255, 255, 0.12))',
          borderRadius: '16px',
          width: '100%',
          maxWidth: '480px',
          padding: '24px',
          boxShadow: '0 20px 40px rgba(0,0,0,0.6)',
          position: 'relative',
        }}
      >
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '18px',
            right: '18px',
            background: 'none',
            border: 'none',
            color: 'var(--muted-text, #94a3b8)',
            cursor: 'pointer',
          }}
        >
          <X size={20} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <div
            style={{
              padding: '10px',
              borderRadius: '10px',
              backgroundColor: 'rgba(0, 229, 255, 0.1)',
              color: 'var(--ion-cyan, #00e5ff)',
            }}
          >
            <Smartphone size={24} />
          </div>
          <div>
            <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600 }}>Pair Android Device</h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--muted-text, #94a3b8)' }}>
              Establish authenticated relay connection with ContextGuard mobile app
            </p>
          </div>
        </div>

        {!claimedSessionId ? (
          <form onSubmit={handleClaim}>
            <div style={{ marginBottom: '18px' }}>
              <label
                style={{
                  display: 'block',
                  fontSize: '12px',
                  fontWeight: 600,
                  marginBottom: '8px',
                  color: 'var(--muted-text, #94a3b8)',
                }}
              >
                ONE-TIME PAIRING CODE
              </label>
              <div style={{ position: 'relative' }}>
                <KeyRound
                  size={18}
                  style={{
                    position: 'absolute',
                    left: '14px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: 'var(--muted-text, #94a3b8)',
                  }}
                />
                <input
                  type="text"
                  placeholder="e.g. 7K2M9X"
                  value={pairingCode}
                  onChange={(e) => setPairingCode(e.target.value.toUpperCase())}
                  maxLength={6}
                  style={{
                    width: '100%',
                    padding: '12px 14px 12px 42px',
                    backgroundColor: 'rgba(255, 255, 255, 0.04)',
                    border: '1px solid var(--border-color, rgba(255, 255, 255, 0.15))',
                    borderRadius: '10px',
                    color: '#fff',
                    fontSize: '18px',
                    letterSpacing: '4px',
                    fontWeight: 700,
                    fontFamily: 'monospace',
                    boxSizing: 'border-box',
                  }}
                />
              </div>
              <p style={{ fontSize: '11px', color: 'var(--muted-text, #94a3b8)', marginTop: '8px' }}>
                Open ContextGuard on phone → Supervisor Viva Mode → Connect Supervisor Dashboard to view code.
              </p>
            </div>

            {error && (
              <div
                style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  backgroundColor: 'rgba(239, 68, 68, 0.15)',
                  color: '#ef4444',
                  fontSize: '12px',
                  marginBottom: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <AlertCircle size={16} />
                <span>{error}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              style={{
                width: '100%',
                padding: '12px',
                backgroundColor: 'var(--ion-cyan, #00e5ff)',
                color: '#0a0e17',
                border: 'none',
                borderRadius: '10px',
                fontWeight: 700,
                fontSize: '14px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.7 : 1,
              }}
            >
              {loading ? 'Claiming Session...' : 'Claim Device Connection'}
            </button>
          </form>
        ) : (
          <div style={{ textAlign: 'center', padding: '16px 0' }}>
            {pollingStatus === 'WAITING_APPROVAL' && (
              <div>
                <Clock className="animate-spin" size={36} color="var(--ask, #eab308)" style={{ margin: '0 auto 12px' }} />
                <h4 style={{ margin: '0 0 6px 0', fontSize: '16px' }}>Approval Pending on Phone</h4>
                <p style={{ fontSize: '13px', color: 'var(--muted-text, #94a3b8)', margin: '0 0 16px 0' }}>
                  Target: <strong>{claimedDevice?.name}</strong> ({claimedDevice?.model})
                  <br />
                  Please tap <strong>"Approve"</strong> on your phone screen to complete pairing.
                </p>
              </div>
            )}

            {pollingStatus === 'APPROVED' && (
              <div>
                <CheckCircle size={40} color="var(--act, #22c55e)" style={{ margin: '0 auto 12px' }} />
                <h4 style={{ margin: '0 0 6px 0', fontSize: '16px', color: 'var(--act, #22c55e)' }}>Pairing Approved!</h4>
                <p style={{ fontSize: '13px', color: 'var(--muted-text, #94a3b8)', margin: 0 }}>
                  Scoped credentials established. Launching real-time telemetry stream...
                </p>
              </div>
            )}

            {error && (
              <div
                style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  backgroundColor: 'rgba(239, 68, 68, 0.15)',
                  color: '#ef4444',
                  fontSize: '12px',
                  marginTop: '16px',
                }}
              >
                {error}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
