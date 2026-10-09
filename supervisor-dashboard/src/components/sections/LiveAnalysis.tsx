import React, { useState } from 'react';
import {
  Zap,
  Play,
  CheckCircle,
  AlertTriangle,
  HelpCircle,
  XCircle,
  Clock,
  Layers,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';
import type { BaseArtifact, AnalysisResponse, AnalysisRequest } from '../../types';
import { runLiveAnalysis } from '../../services/api';

interface LiveAnalysisProps {
  artifacts: BaseArtifact[];
  onAnalysisComplete?: (result: AnalysisResponse) => void;
  latestResult: AnalysisResponse | null;
}

const ACTION_OPTIONS = [
  'SHARE_TO_PUBLIC_FORUM',
  'SEND_TO_UNVERIFIED_RECIPIENT',
  'UPLOAD_TO_CLOUD_STORAGE',
  'SAVE_TO_OFFLINE_SECURE_VAULT',
  'SUBMIT_FORM_VIA_BROWSER',
  'VERIFY_IDENTITY_VIA_OFFICIAL_PORTAL',
  'EXECUTE_UPI_TRANSFER',
  'SHARE_INTERNALLY_WITH_FAMILY',
];

const RECIPIENT_OPTIONS = [
  'Unknown Public Group (@crypto_alerts_tg)',
  'Public Discussion Forum (Reddit/X)',
  'Official Bank Portal (hdfcbank.com)',
  'Verified Personal Contact (Family)',
  'Automated Discord Webhook',
  'Unverified Customer Support Bot',
];

const DESTINATION_OPTIONS = [
  'Telegram Channel (Public Broadcast)',
  'Discord Server (#general)',
  'Google Drive (Shared Link)',
  'Encrypted Local Storage (/vault)',
  'WhatsApp DM (End-to-End Encrypted)',
  'Web Browser Form Submission',
];

export const LiveAnalysis: React.FC<LiveAnalysisProps> = ({
  artifacts,
  onAnalysisComplete,
  latestResult,
}) => {
  const [selectedArtifactId, setSelectedArtifactId] = useState<string>(
    artifacts[0]?.id || 'ART-FIN-001'
  );
  const [selectedAction, setSelectedAction] = useState<string>(ACTION_OPTIONS[0]);
  const [recipient, setRecipient] = useState<string>(RECIPIENT_OPTIONS[0]);
  const [destination, setDestination] = useState<string>(DESTINATION_OPTIONS[0]);
  const [sourceApp, setSourceApp] = useState<string>('Telegram Messenger');

  const [analyzing, setAnalyzing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AnalysisResponse | null>(latestResult);

  const currentArtifact = artifacts.find((a) => a.id === selectedArtifactId) || artifacts[0];

  const handleRunAnalysis = async () => {
    setAnalyzing(true);
    setError(null);
    try {
      const payload: AnalysisRequest = {
        artifact: selectedArtifactId,
        action: selectedAction,
        recipient: recipient,
        destination: destination,
        source_app: sourceApp,
      };
      const res = await runLiveAnalysis(payload);
      setResult(res);
      if (onAnalysisComplete) {
        onAnalysisComplete(res);
      }
    } catch (err: any) {
      setError(err?.message || 'Analysis failed to execute');
    } finally {
      setAnalyzing(false);
    }
  };

  const getInterventionBadge = (intervention: string) => {
    switch (intervention.toUpperCase()) {
      case 'STOP':
        return (
          <span className="badge badge-stop glow-rose" style={{ fontSize: '14px', padding: '6px 14px' }}>
            <XCircle size={16} style={{ marginRight: '6px' }} /> STOP INTERVENTION
          </span>
        );
      case 'WARN':
        return (
          <span className="badge badge-warn" style={{ fontSize: '14px', padding: '6px 14px' }}>
            <AlertTriangle size={16} style={{ marginRight: '6px' }} /> WARN USER
          </span>
        );
      case 'ASK':
        return (
          <span className="badge badge-ask" style={{ fontSize: '14px', padding: '6px 14px' }}>
            <HelpCircle size={16} style={{ marginRight: '6px' }} /> ASK CONFIRMATION
          </span>
        );
      case 'ACT':
        return (
          <span className="badge badge-act glow-emerald" style={{ fontSize: '14px', padding: '6px 14px' }}>
            <CheckCircle size={16} style={{ marginRight: '6px' }} /> ACT PERMITTED
          </span>
        );
      default:
        return <span className="badge badge-not-eval">{intervention}</span>;
    }
  };

  return (
    <section id="section-live-analysis" style={{ marginBottom: '32px' }}>
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: '16px'
      }}>
        <div>
          <h2 style={{
            fontSize: '18px',
            fontWeight: 700,
            color: '#f8fafc',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            margin: 0
          }}>
            <Zap size={18} color="#f59e0b" />
            2. Live Analysis Console
          </h2>
          <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0 0' }}>
            Interactive supervisor sandbox executing the same 6-stage reasoning pipeline deployed to the Android client.
          </p>
        </div>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'minmax(340px, 1fr) minmax(420px, 1.3fr)',
        gap: '16px'
      }}>
        {/* Left: Input Selection Form */}
        <div className="card" style={{ padding: '20px', backgroundColor: '#0c111e' }}>
          <div style={{
            fontSize: '13px',
            fontWeight: 600,
            color: '#38bdf8',
            marginBottom: '14px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}>
            <Layers size={14} />
            PARAMETERS & INTENT CONTEXT
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {/* Artifact Selector */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', color: '#94a3b8', marginBottom: '6px' }}>
                Synthetic Base Artifact (EARB 20 Set):
              </label>
              <select
                value={selectedArtifactId}
                onChange={(e) => setSelectedArtifactId(e.target.value)}
                style={{ width: '100%' }}
              >
                {artifacts.map((art) => (
                  <option key={art.id} value={art.id}>
                    [{art.category}] {art.id} — {art.ocr_preview.slice(0, 45)}...
                  </option>
                ))}
              </select>
              {currentArtifact && (
                <div style={{
                  marginTop: '6px',
                  padding: '8px',
                  backgroundColor: '#090d16',
                  borderRadius: '4px',
                  border: '1px solid #141c2c',
                  fontSize: '11px',
                  color: '#64748b',
                  fontFamily: 'monospace'
                }}>
                  Detected PII: {currentArtifact.detected_pii.join(', ') || 'None'} | Faces: {currentArtifact.detected_faces}
                </div>
              )}
            </div>

            {/* Action Selector */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', color: '#94a3b8', marginBottom: '6px' }}>
                Intended Action (Pre-Action Candidate):
              </label>
              <select
                value={selectedAction}
                onChange={(e) => setSelectedAction(e.target.value)}
                style={{ width: '100%' }}
              >
                {ACTION_OPTIONS.map((act) => (
                  <option key={act} value={act}>
                    {act}
                  </option>
                ))}
              </select>
            </div>

            {/* Recipient */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', color: '#94a3b8', marginBottom: '6px' }}>
                Target Recipient:
              </label>
              <select
                value={recipient}
                onChange={(e) => setRecipient(e.target.value)}
                style={{ width: '100%' }}
              >
                {RECIPIENT_OPTIONS.map((rec) => (
                  <option key={rec} value={rec}>
                    {rec}
                  </option>
                ))}
              </select>
            </div>

            {/* Destination */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', color: '#94a3b8', marginBottom: '6px' }}>
                Target Destination Channel:
              </label>
              <select
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
                style={{ width: '100%' }}
              >
                {DESTINATION_OPTIONS.map((dst) => (
                  <option key={dst} value={dst}>
                    {dst}
                  </option>
                ))}
              </select>
            </div>

            {/* Source App */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', color: '#94a3b8', marginBottom: '6px' }}>
                Source Mobile Application:
              </label>
              <input
                type="text"
                value={sourceApp}
                onChange={(e) => setSourceApp(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>

            {/* Action Button */}
            <button
              onClick={handleRunAnalysis}
              disabled={analyzing}
              className="btn btn-primary"
              style={{
                marginTop: '10px',
                padding: '12px',
                width: '100%',
                fontWeight: 600,
                letterSpacing: '0.03em',
              }}
            >
              {analyzing ? (
                <>
                  <Clock size={16} className="animate-spin" />
                  EVALUATING PIPELINE...
                </>
              ) : (
                <>
                  <Play size={16} />
                  EXECUTE SAFETY PIPELINE
                </>
              )}
            </button>

            {error && (
              <div style={{
                padding: '10px',
                backgroundColor: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                borderRadius: '6px',
                color: '#f87171',
                fontSize: '12px'
              }}>
                {error}
              </div>
            )}
          </div>
        </div>

        {/* Right: Real-time Analysis Telemetry Output */}
        <div className="card" style={{ padding: '20px', backgroundColor: '#0c111e' }}>
          <div style={{
            fontSize: '13px',
            fontWeight: 600,
            color: '#38bdf8',
            marginBottom: '14px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <ShieldAlert size={14} />
              REAL-TIME PIPELINE INFERENCE OUTCOME
            </div>
            {result && (
              <span style={{ fontSize: '11px', color: '#64748b', fontFamily: 'monospace' }}>
                {result.latency_ms || 32.4} ms
              </span>
            )}
          </div>

          {!result && !analyzing && (
            <div style={{
              height: '320px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#475569',
              textAlign: 'center',
              padding: '20px'
            }}>
              <Zap size={36} color="#1e293b" style={{ marginBottom: '12px' }} />
              <div style={{ fontSize: '13px', color: '#64748b', fontWeight: 500 }}>
                Select parameters on the left and run analysis.
              </div>
              <div style={{ fontSize: '11px', color: '#475569', marginTop: '4px' }}>
                Pipeline evaluates artifact features, recipient, reversibility penalty, and produces deterministic intervention.
              </div>
            </div>
          )}

          {analyzing && (
            <div style={{
              height: '320px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#38bdf8'
            }}>
              <Clock size={36} className="animate-spin" style={{ marginBottom: '16px' }} />
              <div style={{ fontSize: '13px', fontWeight: 600 }}>Executing 6-Stage Reasoning...</div>
              <div style={{ fontSize: '11px', color: '#64748b', marginTop: '6px' }}>
                Context → Intent → Evidence → Consequence → Uncertainty → Policy
              </div>
            </div>
          )}

          {result && !analyzing && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {/* Primary Intervention Badge & Reason */}
              <div style={{
                padding: '16px',
                backgroundColor: '#090d16',
                borderRadius: '8px',
                border: '1px solid #19243a',
                display: 'flex',
                flexDirection: 'column',
                gap: '10px'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: '12px', color: '#64748b', textTransform: 'uppercase' }}>Intervention:</span>
                  {getInterventionBadge(result.intervention)}
                </div>
                <div style={{ fontSize: '13px', color: '#e2e8f0', lineHeight: 1.5 }}>
                  {result.reason}
                </div>
              </div>

              {/* Telemetry Metrics: Risk, Confidence, Latency */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: '10px'
              }}>
                <div style={{
                  padding: '12px',
                  backgroundColor: '#090d16',
                  borderRadius: '6px',
                  border: '1px solid #141c2e'
                }}>
                  <div style={{ fontSize: '11px', color: '#64748b' }}>RISK SCORE (ρ)</div>
                  <div style={{
                    fontSize: '20px',
                    fontFamily: 'monospace',
                    fontWeight: 700,
                    color: result.risk_score > 0.65 ? '#f87171' : result.risk_score > 0.35 ? '#fbbf24' : '#34d399',
                    marginTop: '2px'
                  }}>
                    {result.risk_score.toFixed(4)}
                  </div>
                  <div style={{
                    width: '100%',
                    height: '4px',
                    backgroundColor: '#1e293b',
                    borderRadius: '2px',
                    marginTop: '6px',
                    overflow: 'hidden'
                  }}>
                    <div style={{
                      width: `${Math.min(100, result.risk_score * 100)}%`,
                      height: '100%',
                      backgroundColor: result.risk_score > 0.65 ? '#ef4444' : result.risk_score > 0.35 ? '#f59e0b' : '#10b981',
                    }} />
                  </div>
                </div>

                <div style={{
                  padding: '12px',
                  backgroundColor: '#090d16',
                  borderRadius: '6px',
                  border: '1px solid #141c2e'
                }}>
                  <div style={{ fontSize: '11px', color: '#64748b' }}>CONFIDENCE</div>
                  <div style={{
                    fontSize: '20px',
                    fontFamily: 'monospace',
                    fontWeight: 700,
                    color: '#38bdf8',
                    marginTop: '2px'
                  }}>
                    {((result.confidence || 0.95) * 100).toFixed(1)}%
                  </div>
                  <div style={{
                    width: '100%',
                    height: '4px',
                    backgroundColor: '#1e293b',
                    borderRadius: '2px',
                    marginTop: '6px',
                    overflow: 'hidden'
                  }}>
                    <div style={{
                      width: `${(result.confidence || 0.95) * 100}%`,
                      height: '100%',
                      backgroundColor: '#38bdf8',
                    }} />
                  </div>
                </div>

                <div style={{
                  padding: '12px',
                  backgroundColor: '#090d16',
                  borderRadius: '6px',
                  border: '1px solid #141c2e'
                }}>
                  <div style={{ fontSize: '11px', color: '#64748b' }}>LATENCY</div>
                  <div style={{
                    fontSize: '20px',
                    fontFamily: 'monospace',
                    fontWeight: 700,
                    color: '#cbd5e1',
                    marginTop: '2px'
                  }}>
                    {result.latency_ms || 32.4} ms
                  </div>
                  <div style={{ fontSize: '10px', color: '#64748b', marginTop: '6px' }}>
                    P90 Budget: 3500ms
                  </div>
                </div>
              </div>

              {/* Model Path & Network Mode */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '10px',
                fontSize: '12px'
              }}>
                <div style={{
                  padding: '10px',
                  backgroundColor: '#090d16',
                  borderRadius: '6px',
                  border: '1px solid #141c2e'
                }}>
                  <span style={{ color: '#64748b', display: 'block', fontSize: '11px' }}>MODEL PATH</span>
                  <span style={{ color: '#cbd5e1', fontFamily: 'monospace', fontWeight: 600 }}>
                    {result.model_path || 'Qwen 2.5-VL 3B + XGBoost Hybrid'}
                  </span>
                </div>
                <div style={{
                  padding: '10px',
                  backgroundColor: '#090d16',
                  borderRadius: '6px',
                  border: '1px solid #141c2e'
                }}>
                  <span style={{ color: '#64748b', display: 'block', fontSize: '11px' }}>NETWORK MODE</span>
                  <span style={{ color: '#38bdf8', fontFamily: 'monospace', fontWeight: 600 }}>
                    {result.network_mode || 'REDACTED_LOCAL_BACKEND'}
                  </span>
                </div>
              </div>

              {/* Evidence Grounding List */}
              <div style={{
                padding: '12px',
                backgroundColor: '#090d16',
                borderRadius: '6px',
                border: '1px solid #141c2e'
              }}>
                <div style={{ fontSize: '11px', color: '#64748b', marginBottom: '8px', fontWeight: 600 }}>
                  GROUNDED EVIDENCE ITEMS ({result.evidence?.length || 0})
                </div>
                {result.evidence && result.evidence.length > 0 ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    {result.evidence.map((item: any, i: number) => (
                      <div key={i} style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '8px',
                        fontSize: '12px',
                        color: '#cbd5e1'
                      }}>
                        <ArrowRight size={13} color="#38bdf8" style={{ marginTop: '3px', flexShrink: 0 }} />
                        <span>
                          <strong style={{ color: '#38bdf8' }}>[{typeof item === 'string' ? 'EVIDENCE' : item.type || 'SIGNAL'}]</strong>{' '}
                          {typeof item === 'string' ? item : item.description || item.claim}
                        </span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div style={{ fontSize: '12px', color: '#64748b' }}>No sensitive risk factors triggered. Safe baseline action.</div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
};
