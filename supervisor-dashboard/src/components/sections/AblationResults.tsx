import React from 'react';
import { Sliders, CheckCircle2 } from 'lucide-react';
import type { EvaluationResults } from '../../types';

interface AblationResultsProps {
  results: EvaluationResults | null;
  loading?: boolean;
}

export const AblationResults: React.FC<AblationResultsProps> = ({ results }) => {
  const ablations = results?.ablations || {};

  const ablationRows = [
    { key: 'a1', id: 'A1', name: 'A1: No Intent', desc: 'Ablates intended action and recipient signals completely' },
    { key: 'a2', id: 'A2', name: 'A2: No Multimodality', desc: 'Ablates visual reasoning layers, keeping only OCR text tokens' },
    { key: 'a3', id: 'A3', name: 'A3: Fixed Policy (λ=0)', desc: 'Ablates reversibility penalization, treating all harm as static' },
    { key: 'a4', id: 'A4', name: 'A4: Adaptive Policy (λ=0.75)', desc: 'Full calibrated policy with dynamic reversibility penalty' },
    { key: 'a5', id: 'A5', name: 'A5: Warn Everything', desc: 'Extreme naive baseline issuing unconditional warnings' },
  ];

  return (
    <section id="section-ablations" style={{ marginBottom: '32px' }}>
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
            <Sliders size={18} color="#f59e0b" />
            7. Component Ablation Study
          </h2>
          <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0 0' }}>
            Isolates the contribution of intent conditioning, visual reasoning, and adaptive reversibility penalization.
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
          5 ABLATION CONDITIONS
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Ablation Variant</th>
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
            {ablationRows.map((row) => {
              const item = ablations[row.key];
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
