import React from 'react';
import { BarChart3, CheckCircle2 } from 'lucide-react';
import type { EvaluationResults } from '../../types';

interface BaselineComparisonProps {
  results: EvaluationResults | null;
  loading?: boolean;
}

export const BaselineComparison: React.FC<BaselineComparisonProps> = ({ results }) => {
  const baselines = results?.baselines || {};

  const baselineRows = [
    { key: 'b1', id: 'B1', name: 'B1: Artifact-Only', desc: 'Predicts risk without user action or destination signals' },
    { key: 'b2', id: 'B2', name: 'B2: Text/OCR-Only', desc: 'Text-only heuristic without visual reasoning or grounding' },
    { key: 'b3', id: 'B3', name: 'B3: Multimodal (No Intent)', desc: 'Multimodal vision model without explicit intent conditioning' },
    { key: 'b4', id: 'B4', name: 'B4: Intent + Fixed Threshold', desc: 'Action-conditioned without reversibility penalization (λ=0)' },
    { key: 'b5', id: 'B5', name: 'B5: ContextGuard (Oracle Intent)', desc: 'Full 6-stage pipeline with explicit user intent & λ=0.75' },
    { key: 'b6', id: 'B6', name: 'B6: ContextGuard (Inferred Intent)', desc: 'Full 6-stage pipeline with inferred candidate intent & λ=0.75' },
  ];

  return (
    <section id="section-baselines" style={{ marginBottom: '32px' }}>
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
            <BarChart3 size={18} color="#38bdf8" />
            6. Baseline Comparison Matrix
          </h2>
          <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0 0' }}>
            Empirical evaluation across EARB 60 pairs. Missing experiments strictly display "Not evaluated".
          </p>
        </div>
        <div style={{
          fontSize: '11px',
          color: '#cbd5e1',
          fontFamily: 'monospace',
          backgroundColor: '#0c121e',
          padding: '4px 10px',
          borderRadius: '4px',
          border: '1px solid #1a253a'
        }}>
          N = 60 PAIRS // STRICT EMPIRICAL
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Baseline System</th>
              <th>Status</th>
              <th>Accuracy</th>
              <th>Macro F1</th>
              <th>STOP Recall</th>
              <th>ACT FAR (False Alarms)</th>
              <th>ECE</th>
              <th>Mean Latency</th>
              <th>95% Confidence Interval</th>
            </tr>
          </thead>
          <tbody>
            {baselineRows.map((row) => {
              const item = baselines[row.key];
              const isEvaluated = item && item.status === 'COMPLETED' && item.metrics;
              const m = item?.metrics;

              return (
                <tr key={row.key}>
                  <td style={{ minWidth: '220px' }}>
                    <div style={{ fontWeight: 600, color: isEvaluated ? '#f8fafc' : '#94a3b8' }}>
                      {row.name}
                    </div>
                    <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px' }}>
                      {row.desc}
                    </div>
                  </td>
                  <td>
                    {isEvaluated ? (
                      <span className="badge badge-operational">
                        <CheckCircle2 size={10} style={{ marginRight: '3px' }} />
                        EVALUATED
                      </span>
                    ) : (
                      <span className="badge badge-not-eval">
                        Not evaluated
                      </span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'monospace' }}>
                    {isEvaluated && m?.accuracy !== undefined ? (
                      `${(m.accuracy * 100).toFixed(2)}%`
                    ) : (
                      <span className="badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>
                    {isEvaluated && m?.macro_f1 !== undefined ? (
                      <span style={{ color: m.macro_f1 >= 0.5 ? '#34d399' : '#cbd5e1' }}>
                        {m.macro_f1.toFixed(4)}
                      </span>
                    ) : (
                      <span className="badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'monospace' }}>
                    {isEvaluated && m?.stop_recall !== undefined ? (
                      <span style={{ color: m.stop_recall >= 0.9 ? '#34d399' : '#fbbf24' }}>
                        {(m.stop_recall * 100).toFixed(2)}%
                      </span>
                    ) : (
                      <span className="badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'monospace' }}>
                    {isEvaluated && m?.act_alarm_rate !== undefined ? (
                      <span style={{ color: m.act_alarm_rate <= 0.15 ? '#34d399' : '#f87171' }}>
                        {(m.act_alarm_rate * 100).toFixed(2)}%
                      </span>
                    ) : (
                      <span className="badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'monospace' }}>
                    {isEvaluated && m?.ece !== undefined ? (
                      m.ece.toFixed(4)
                    ) : (
                      <span className="badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'monospace' }}>
                    {isEvaluated && m?.mean_latency_ms !== undefined ? (
                      `${m.mean_latency_ms.toFixed(2)} ms`
                    ) : (
                      <span className="badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'monospace', fontSize: '11px', color: '#94a3b8' }}>
                    {isEvaluated && m?.macro_f1_ci_95 ? (
                      `[${m.macro_f1_ci_95[0]?.toFixed(4)}, ${m.macro_f1_ci_95[1]?.toFixed(4)}]`
                    ) : (
                      <span className="badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
};
