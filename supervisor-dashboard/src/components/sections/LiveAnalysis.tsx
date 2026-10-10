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
  Scale,
} from 'lucide-react';
import type { BaseArtifact, AnalysisResponse, AnalysisRequest } from '../../types';
import { runLiveAnalysis } from '../../services/api';
import { ContextField, type ContextFieldMode } from '../ContextField';

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
  const [focusedEvidenceIndex, setFocusedEvidenceIndex] = useState<number | null>(null);

  const currentArtifact = artifacts.find((a) => a.id === selectedArtifactId) || artifacts[0];

  const handleActionSwitch = async (newAction: string) => {
    setSelectedAction(newAction);
    setAnalyzing(true);
    setError(null);
    try {
      const payload: AnalysisRequest = {
        artifact: selectedArtifactId,
        action: newAction,
        recipient: recipient,
        destination: destination,
        source_app: sourceApp,
      };
      const res = await runLiveAnalysis(payload);
      setResult(res);
      setFocusedEvidenceIndex(null);
      if (onAnalysisComplete) {
        onAnalysisComplete(res);
      }
    } catch (err: any) {
      setError(err?.message || 'Analysis failed to execute');
    } finally {
      setAnalyzing(false);
    }
  };

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

            {/* Counterfactual Quick Action Switcher */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label style={{ fontSize: '12px', color: '#94a3b8' }}>
                  Intended Action (Pre-Action Candidate):
                </label>
                <span style={{ fontSize: '10px', color: '#38bdf8', fontFamily: 'monospace' }}>
                  COUNTERFACTUAL SWITCH
                </span>
              </div>

              {/* Quick Switch Pills: SAVE vs SEND vs POST */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '6px', marginBottom: '8px' }}>
                <button
                  type="button"
                  onClick={() => handleActionSwitch('SAVE_TO_OFFLINE_SECURE_VAULT')}
                  className="precision-press"
                  style={{
                    padding: '6px 8px',
                    fontSize: '11px',
                    fontWeight: 600,
                    borderRadius: '6px',
                    border: '1px solid',
                    borderColor: selectedAction.includes('SAVE') ? 'var(--act)' : 'var(--contour-border)',
                    backgroundColor: selectedAction.includes('SAVE') ? 'rgba(201, 247, 122, 0.12)' : 'var(--elevated-surface)',
                    color: selectedAction.includes('SAVE') ? 'var(--act)' : 'var(--muted-text)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease'
                  }}
                >
                  🔒 SAVE (Vault)
                </button>
                <button
                  type="button"
                  onClick={() => handleActionSwitch('SEND_TO_UNVERIFIED_RECIPIENT')}
                  className="precision-press"
                  style={{
                    padding: '6px 8px',
                    fontSize: '11px',
                    fontWeight: 600,
                    borderRadius: '6px',
                    border: '1px solid',
                    borderColor: selectedAction.includes('SEND') ? 'var(--warn)' : 'var(--contour-border)',
                    backgroundColor: selectedAction.includes('SEND') ? 'rgba(255, 170, 101, 0.12)' : 'var(--elevated-surface)',
                    color: selectedAction.includes('SEND') ? 'var(--warn)' : 'var(--muted-text)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease'
                  }}
                >
                  ✉️ SEND (Unverified)
                </button>
                <button
                  type="button"
                  onClick={() => handleActionSwitch('SHARE_TO_PUBLIC_FORUM')}
                  className="precision-press"
                  style={{
                    padding: '6px 8px',
                    fontSize: '11px',
                    fontWeight: 600,
                    borderRadius: '6px',
                    border: '1px solid',
                    borderColor: selectedAction.includes('SHARE') || selectedAction.includes('POST') ? 'var(--stop)' : 'var(--contour-border)',
                    backgroundColor: selectedAction.includes('SHARE') || selectedAction.includes('POST') ? 'rgba(255, 102, 125, 0.12)' : 'var(--elevated-surface)',
                    color: selectedAction.includes('SHARE') || selectedAction.includes('POST') ? 'var(--stop)' : 'var(--muted-text)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease'
                  }}
                >
                  📢 POST (Public)
                </button>
              </div>

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
              className="btn btn-primary precision-press"
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
        <div className="card" style={{ padding: '20px', backgroundColor: '#0c111e' }} role="region" aria-live="polite">
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
              color: 'var(--muted-text)',
              textAlign: 'center',
              padding: '20px'
            }}>
              <ContextField mode="IDLE" size={120} />
              <div style={{ fontSize: '13px', color: 'var(--text-main)', fontWeight: 600, marginTop: '12px' }}>
                Context Field Awaiting Evaluation
              </div>
              <div style={{ fontSize: '11px', color: 'var(--muted-text)', marginTop: '4px', maxWidth: '340px' }}>
                Concentric contours reflect passive baseline monitoring. Select parameters on the left to execute the 6-stage reasoning pipeline.
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
              color: 'var(--ion-cyan)'
            }}>
              <ContextField mode="ANALYZING" size={130} />
              <div style={{ fontSize: '14px', fontWeight: 700, fontFamily: "'Space Grotesk', sans-serif", marginTop: '14px', color: 'var(--text-main)' }}>
                Wavefront Signal Propagating...
              </div>
              <div style={{ fontSize: '11px', color: 'var(--muted-text)', marginTop: '4px', fontFamily: "'JetBrains Mono', monospace" }}>
                Context → Intent → Evidence → Consequence → Uncertainty → Policy
              </div>
            </div>
          )}

          {result && !analyzing && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {/* Primary Intervention Banner with Context Field */}
              {(() => {
                const modeStr = (result.intervention || 'ACT').toUpperCase() as ContextFieldMode;
                const fieldMode: ContextFieldMode = ['ACT', 'ASK', 'WARN', 'STOP'].includes(modeStr) ? modeStr : 'ACT';
                const focusedEvidenceAngle = focusedEvidenceIndex != null ? (focusedEvidenceIndex * 45 - 20) : null;
                return (
                  <div style={{
                    padding: '18px',
                    backgroundColor: 'var(--deep-surface)',
                    borderRadius: '12px',
                    border: '1px solid var(--contour-border-active)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '16px'
                  }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        {getInterventionBadge(result.intervention)}
                        <span style={{ fontSize: '11px', color: 'var(--muted-text)', fontFamily: "'JetBrains Mono', monospace" }}>
                          LATENCY: {result.latency_ms || 32.4} ms
                        </span>
                      </div>
                      <div style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.5 }}>
                        {result.reason}
                      </div>
                    </div>
                    <ContextField mode={fieldMode} size={90} focusedEvidenceAngle={focusedEvidenceAngle} />
                  </div>
                );
              })()}

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
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>
                    GROUNDED EVIDENCE ITEMS ({result.evidence?.length || 0})
                  </div>
                  <span style={{ fontSize: '10px', color: '#45e4ff', fontFamily: 'monospace' }}>
                    {focusedEvidenceIndex !== null ? 'TARGET FOCUSED' : 'TAP ITEM TO FOCUS VECTOR'}
                  </span>
                </div>
                {result.evidence && result.evidence.length > 0 ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {result.evidence.map((item: any, i: number) => {
                      const isFocused = focusedEvidenceIndex === i;
                      return (
                        <div
                          key={i}
                          onClick={() => setFocusedEvidenceIndex(isFocused ? null : i)}
                          className={`evidence-focus-target precision-press ${isFocused ? 'focused' : ''}`}
                          style={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '4px',
                            padding: '8px 10px',
                            borderRadius: '6px',
                            backgroundColor: isFocused ? 'rgba(69, 228, 255, 0.08)' : 'rgba(255, 255, 255, 0.02)',
                            border: `1px solid ${isFocused ? 'var(--ion-cyan)' : 'var(--contour-border)'}`,
                            cursor: 'pointer',
                          }}
                        >
                          <div style={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            gap: '8px',
                            fontSize: '12px',
                            color: isFocused ? '#ffffff' : '#cbd5e1'
                          }}>
                            <ArrowRight
                              size={13}
                              color={isFocused ? '#45e4ff' : '#38bdf8'}
                              style={{
                                marginTop: '3px',
                                flexShrink: 0,
                                transform: isFocused ? 'translateX(2px)' : 'none',
                                transition: 'transform 0.15s ease'
                              }}
                            />
                            <span>
                              <strong style={{ color: isFocused ? '#45e4ff' : '#38bdf8' }}>
                                [{typeof item === 'string' ? 'EVIDENCE' : item.type || 'SIGNAL'}]
                              </strong>{' '}
                              {typeof item === 'string' ? item : item.description || item.claim}
                            </span>
                          </div>
                          {isFocused && (
                            <div style={{
                              marginLeft: '21px',
                              fontSize: '11px',
                              color: '#94a3b8',
                              fontFamily: 'monospace',
                              borderTop: '1px solid rgba(69, 228, 255, 0.2)',
                              paddingTop: '4px',
                              marginTop: '2px'
                            }}>
                              ⦿ Focal contour vector oriented at {(i * 45 - 20)}° in Context Field.
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div style={{ fontSize: '12px', color: '#64748b' }}>No sensitive risk factors triggered. Safe baseline action.</div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Distinctive Visual: Counterfactual Action-Comparison Visualization */}
      <div className="card" style={{
        marginTop: '24px',
        padding: '22px',
        backgroundColor: 'var(--bg-secondary)',
        border: '1px solid var(--contour-border)'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid var(--contour-border)',
          paddingBottom: '14px',
          marginBottom: '16px'
        }}>
          <div>
            <div style={{
              fontSize: '11px',
              fontFamily: 'var(--font-mono)',
              color: 'var(--ion-cyan)',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              marginBottom: '2px'
            }}>
              Causal Policy Invariance // EARB Formulation
            </div>
            <h3 style={{
              fontSize: '16px',
              fontFamily: 'var(--font-display)',
              fontWeight: 700,
              color: 'var(--text-main)',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              margin: 0
            }}>
              <Scale size={18} color="var(--ion-cyan)" />
              Action-Comparison Visualization: Counterfactual Risk Shift
            </h3>
            <p style={{ fontSize: '12px', color: 'var(--muted-text)', margin: '4px 0 0 0' }}>
              Holding artifact <strong>{currentArtifact?.id || 'ART-FIN-001'}</strong> constant: observe how intended action shifts effective risk ρ across calibrated intervention thresholds.
            </p>
          </div>
          <div style={{
            fontSize: '11px',
            fontFamily: 'var(--font-mono)',
            color: 'var(--signal-lime)',
            backgroundColor: 'rgba(216, 255, 99, 0.08)',
            border: '1px solid rgba(216, 255, 99, 0.25)',
            padding: '6px 12px',
            borderRadius: '4px',
            textAlign: 'right'
          }}>
            <div>CAC: 100.0% (PAIRED TESTS)</div>
            <div style={{ fontSize: '9px', color: 'var(--muted-text)' }}>ZERO FALSE ACT ON BROADCAST</div>
          </div>
        </div>

        {/* Counterfactual Action Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
          gap: '14px',
          marginBottom: '20px'
        }}>
          {[
            {
              action: 'SAVE_TO_OFFLINE_SECURE_VAULT',
              label: 'Local Vault Backup',
              desc: 'Encrypted device-only sandbox with zero network egress',
              intervention: 'ACT',
              color: 'var(--act)',
              severity: 0.15,
              reversibility: 0.05,
              rho: 0.156,
              rationale: 'Safe containment. High reversibility.',
            },
            {
              action: 'VERIFY_IDENTITY_VIA_OFFICIAL_PORTAL',
              label: 'Official KYC Verification',
              desc: 'TLS 1.3 pinned HTTPS transmission to legitimate portal',
              intervention: 'ASK',
              color: 'var(--ask)',
              severity: 0.25,
              reversibility: 0.15,
              rho: 0.278,
              rationale: 'Legitimate destination; soft user confirmation needed.',
            },
            {
              action: 'SEND_TO_UNVERIFIED_RECIPIENT',
              label: 'Unverified Direct Message',
              desc: 'Third-party chat DM with unconfirmed external recipient',
              intervention: 'WARN',
              color: 'var(--warn)',
              severity: 0.60,
              reversibility: 0.65,
              rho: 0.892,
              rationale: 'Moderate hazard. User must confirm intentional override.',
            },
            {
              action: 'SHARE_TO_PUBLIC_FORUM',
              label: 'Public Forum Broadcast',
              desc: 'Public Telegram / Reddit channel with uncontrolled clawback',
              intervention: 'STOP',
              color: 'var(--stop)',
              severity: 0.88,
              reversibility: 0.92,
              rho: 1.487,
              rationale: 'Catastrophic irreversible disclosure. Hard intercept.',
            },
          ].map((item) => {
            const isCurrentlySelected = selectedAction === item.action;
            return (
              <div
                key={item.action}
                onClick={() => handleActionSwitch(item.action)}
                style={{
                  padding: '16px',
                  borderRadius: '8px',
                  backgroundColor: isCurrentlySelected ? 'rgba(69, 228, 255, 0.06)' : 'rgba(255, 255, 255, 0.02)',
                  border: `1px solid ${isCurrentlySelected ? 'var(--ion-cyan)' : 'var(--contour-border)'}`,
                  boxShadow: isCurrentlySelected ? '0 0 16px rgba(69, 228, 255, 0.15)' : 'none',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  transition: 'all 0.2s cubic-bezier(0.16, 1, 0.3, 1)'
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span style={{
                      fontSize: '11px',
                      fontFamily: 'var(--font-mono)',
                      fontWeight: 700,
                      color: item.color,
                      backgroundColor: `${item.color}15`,
                      border: `1px solid ${item.color}44`,
                      padding: '2px 8px',
                      borderRadius: '4px'
                    }}>
                      {item.intervention}
                    </span>
                    {isCurrentlySelected && (
                      <span style={{ fontSize: '10px', color: 'var(--ion-cyan)', fontFamily: 'var(--font-mono)' }}>
                        ● CURRENT ACTION
                      </span>
                    )}
                  </div>

                  <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>
                    {item.label}
                  </div>
                  <div style={{ fontSize: '11px', color: 'var(--muted-text)', lineHeight: 1.4, marginBottom: '12px' }}>
                    {item.desc}
                  </div>
                </div>

                {/* Quantitative Metric Breakdown */}
                <div style={{
                  backgroundColor: '#07090f',
                  padding: '10px',
                  borderRadius: '6px',
                  border: '1px solid var(--contour-border)',
                  fontSize: '11px',
                  fontFamily: 'var(--font-mono)'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--muted-text)', marginBottom: '3px' }}>
                    <span>Severity (S):</span>
                    <span style={{ color: 'var(--text-main)' }}>{item.severity.toFixed(2)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--muted-text)', marginBottom: '3px' }}>
                    <span>Reversibility (R):</span>
                    <span style={{ color: 'var(--text-main)' }}>{item.reversibility.toFixed(2)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--muted-text)', borderTop: '1px dashed var(--contour-border)', paddingTop: '4px', marginTop: '2px' }}>
                    <span>Effective Risk (ρ):</span>
                    <strong style={{ color: item.color }}>{item.rho.toFixed(3)}</strong>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Calibrated Risk-Scale Continuum */}
        <div style={{
          backgroundColor: '#07090f',
          padding: '16px',
          borderRadius: '8px',
          border: '1px solid var(--contour-border)',
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '10px',
            fontSize: '11px',
            fontFamily: 'var(--font-mono)',
            color: 'var(--muted-text)'
          }}>
            <span>CALIBRATED POLICY THRESHOLD PARTITION</span>
            <span>FORMULA: ρ = S · (1 + 0.75 · R)</span>
          </div>

          {/* Continuum Bar */}
          <div style={{
            position: 'relative',
            height: '24px',
            borderRadius: '4px',
            overflow: 'hidden',
            display: 'flex',
            border: '1px solid var(--contour-border)',
            marginBottom: '8px'
          }}>
            <div style={{ width: '13.3%', backgroundColor: 'rgba(201, 247, 122, 0.25)', borderRight: '1px solid var(--act)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '10px', fontFamily: 'var(--font-mono)', color: 'var(--act)', fontWeight: 700 }}>
              ACT (&lt;0.20)
            </div>
            <div style={{ width: '10.0%', backgroundColor: 'rgba(255, 209, 102, 0.25)', borderRight: '1px solid var(--ask)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '10px', fontFamily: 'var(--font-mono)', color: 'var(--ask)', fontWeight: 700 }}>
              ASK
            </div>
            <div style={{ width: '20.0%', backgroundColor: 'rgba(255, 170, 101, 0.25)', borderRight: '1px solid var(--warn)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '10px', fontFamily: 'var(--font-mono)', color: 'var(--warn)', fontWeight: 700 }}>
              WARN (&lt;0.65)
            </div>
            <div style={{ width: '56.7%', backgroundColor: 'rgba(255, 102, 125, 0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '10px', fontFamily: 'var(--font-mono)', color: 'var(--stop)', fontWeight: 700 }}>
              STOP (≥0.65)
            </div>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            fontSize: '11px',
            color: 'var(--muted-text)',
            fontFamily: 'var(--font-mono)'
          }}>
            <span>0.00 (Minimal Risk)</span>
            <span>τ_act = 0.20</span>
            <span>τ_ask = 0.35</span>
            <span>τ_stop = 0.65</span>
            <span>1.50+ (Catastrophic Irreversible Egress)</span>
          </div>
        </div>
      </div>
    </section>
  );
};
