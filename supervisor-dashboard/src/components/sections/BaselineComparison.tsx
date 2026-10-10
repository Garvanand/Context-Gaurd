import React, { useState } from 'react';
import {
  BarChart3,
  CheckCircle2,
  Grid,
} from 'lucide-react';
import type { EvaluationResults } from '../../types';

interface BaselineComparisonProps {
  results: EvaluationResults | null;
  loading?: boolean;
}

export const BaselineComparison: React.FC<BaselineComparisonProps> = ({ results: _results }) => {

  // Empirical data from results/RESEARCH_REPORT.md and results/b5/metrics.json
  const baselineRows = [
    {
      key: 'b1',
      id: 'B1',
      name: 'B1: Artifact-Only',
      desc: 'Predicts risk from artifact features alone without user action',
      macroF1: 0.1767,
      ci: [0.125, 0.218],
      accuracy: 0.333,
      stopRecall: 0.90,
      actFar: 0.95,
      ece: 0.5667,
      latency: 0.15,
      evaluated: true,
    },
    {
      key: 'b2',
      id: 'B2',
      name: 'B2: Text/OCR-Only',
      desc: 'Text-only regex heuristic without visual reasoning or grounding',
      macroF1: null,
      ci: null,
      accuracy: null,
      stopRecall: null,
      actFar: null,
      ece: null,
      latency: null,
      evaluated: false,
    },
    {
      key: 'b3',
      id: 'B3',
      name: 'B3: Multimodal (No Intent)',
      desc: 'Multimodal vision model without explicit intent conditioning',
      macroF1: 0.1364,
      ci: [0.091, 0.173],
      accuracy: 0.200,
      stopRecall: 0.40,
      actFar: 1.00,
      ece: 0.2800,
      latency: 0.53,
      evaluated: true,
    },
    {
      key: 'b4',
      id: 'B4',
      name: 'B4: Intent + Fixed Threshold',
      desc: 'Action-conditioned without reversibility penalization (λ=0)',
      macroF1: 0.5455,
      ci: [0.458, 0.620],
      accuracy: 0.717,
      stopRecall: 0.95,
      actFar: 0.10,
      ece: 0.1775,
      latency: 0.30,
      evaluated: true,
    },
    {
      key: 'b5',
      id: 'B5',
      name: 'B5: ContextGuard (Oracle Intent)',
      desc: 'Full 6-stage pipeline with explicit user intent & λ=0.75',
      macroF1: 0.5455,
      ci: [0.427, 0.662],
      accuracy: 0.683,
      stopRecall: 0.95,
      actFar: 0.10,
      ece: 0.1942,
      latency: 2584.34,
      evaluated: true,
    },
    {
      key: 'b6',
      id: 'B6',
      name: 'B6: ContextGuard (Inferred Intent)',
      desc: 'Full 6-stage pipeline with inferred candidate intent & λ=0.75',
      macroF1: 0.4476,
      ci: [0.298, 0.597],
      accuracy: 0.550,
      stopRecall: 0.95,
      actFar: 0.50,
      ece: 0.1950,
      latency: 2582.18,
      evaluated: true,
    },
  ];

  // 4x4 Confusion Matrix for ContextGuard B5 (from results/b5/metrics.json)
  // Rows: Expected (Ground Truth), Columns: Predicted
  const confusionLabels = ['ACT', 'ASK', 'WARN', 'STOP'] as const;
  const confusionData = {
    ACT:  { ACT: 18, ASK: 1, WARN: 1, STOP: 0, total: 20 },
    ASK:  { ACT: 2,  ASK: 2, WARN: 1, STOP: 3, total: 8 },
    WARN: { ACT: 4,  ASK: 4, WARN: 2, STOP: 2, total: 12 },
    STOP: { ACT: 0,  ASK: 1, WARN: 0, STOP: 19, total: 20 },
  };

  const [activeMatrixCell, setActiveMatrixCell] = useState<{ exp: string; pred: string; val: number } | null>(null);

  return (
    <section id="section-baselines" style={{ marginBottom: '32px' }}>
      {/* Editorial Section Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: '20px',
        borderBottom: '1px solid var(--contour-border)',
        paddingBottom: '12px'
      }}>
        <div>
          <div style={{
            fontSize: '11px',
            fontFamily: 'var(--font-mono)',
            color: 'var(--ion-cyan)',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            marginBottom: '4px'
          }}>
            EARB v1.0 Benchmark Suite // 60 Pairs
          </div>
          <h2 style={{
            fontSize: '20px',
            fontFamily: 'var(--font-display)',
            fontWeight: 700,
            color: 'var(--text-main)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            margin: 0
          }}>
            <BarChart3 size={20} color="var(--ion-cyan)" />
            Baseline Evaluation & Safety Trade-offs
          </h2>
          <p style={{ fontSize: '13px', color: 'var(--muted-text)', margin: '4px 0 0 0' }}>
            Empirical evaluation against alternative architectures. Missing results are strictly rendered as "Not evaluated".
          </p>
        </div>
        <div style={{
          fontSize: '11px',
          color: 'var(--text-main)',
          fontFamily: 'var(--font-mono)',
          backgroundColor: 'rgba(255, 255, 255, 0.03)',
          padding: '6px 12px',
          borderRadius: '4px',
          border: '1px solid var(--contour-border)'
        }}>
          N = 60 PAIRS // STRICT CLUSTERED SPLIT
        </div>
      </div>

      {/* Restrained Charting Grid: Macro F1 & Safety Trade-off */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(460px, 1fr))',
        gap: '20px',
        marginBottom: '24px'
      }}>
        {/* Chart 1: Macro-F1 by Baseline with 95% Confidence Intervals */}
        <div className="card" style={{ padding: '20px', backgroundColor: 'var(--bg-secondary)' }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '16px',
            borderBottom: '1px solid var(--contour-border)',
            paddingBottom: '10px'
          }}>
            <div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-main)', fontFamily: 'var(--font-display)' }}>
                Macro-F1 by Baseline Architecture
              </div>
              <div style={{ fontSize: '11px', color: 'var(--muted-text)', fontFamily: 'var(--font-mono)' }}>
                Includes 95% bootstrap confidence interval error bars
              </div>
            </div>
            <span style={{ fontSize: '11px', color: 'var(--ion-cyan)', fontFamily: 'var(--font-mono)' }}>
              HIGHER IS BETTER (MAX 1.0)
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', paddingTop: '4px' }}>
            {baselineRows.map((b) => {
              const maxF1 = 0.8;
              const f1Val = b.macroF1 || 0;
              const barWidth = `${Math.min(100, (f1Val / maxF1) * 100)}%`;
              const isLead = b.id === 'B5' || b.id === 'B4';

              return (
                <div key={b.id} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                    <span style={{ fontWeight: 600, color: b.evaluated ? 'var(--text-main)' : 'var(--muted-text)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span style={{
                        display: 'inline-block',
                        width: '24px',
                        fontSize: '11px',
                        fontFamily: 'var(--font-mono)',
                        color: isLead ? 'var(--ion-cyan)' : 'var(--muted-text)'
                      }}>
                        {b.id}
                      </span>
                      {b.name.replace(`${b.id}: `, '')}
                    </span>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: '11px' }}>
                      {b.evaluated ? (
                        <>
                          <strong style={{ color: isLead ? 'var(--signal-lime)' : 'var(--text-main)' }}>
                            {f1Val.toFixed(4)}
                          </strong>
                          {b.ci && (
                            <span style={{ color: 'var(--muted-text)', marginLeft: '6px' }}>
                              [{b.ci[0].toFixed(3)}, {b.ci[1].toFixed(3)}]
                            </span>
                          )}
                        </>
                      ) : (
                        <span className="badge badge-not-eval" style={{ fontSize: '10px' }}>
                          Not evaluated
                        </span>
                      )}
                    </span>
                  </div>

                  {/* Horizontal Bar with CI whisker */}
                  <div style={{
                    position: 'relative',
                    height: '18px',
                    backgroundColor: 'rgba(255, 255, 255, 0.04)',
                    borderRadius: '4px',
                    border: '1px solid var(--contour-border)',
                    overflow: 'hidden'
                  }}>
                    {b.evaluated ? (
                      <div style={{
                        width: barWidth,
                        height: '100%',
                        backgroundColor: isLead ? 'rgba(69, 228, 255, 0.65)' : 'rgba(139, 112, 255, 0.45)',
                        borderRight: isLead ? '2px solid var(--ion-cyan)' : '2px solid var(--electric-violet)',
                        transition: 'width 0.4s ease'
                      }} />
                    ) : (
                      <div style={{
                        width: '100%',
                        height: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '10px',
                        fontFamily: 'var(--font-mono)',
                        color: 'var(--muted-text)',
                        background: 'repeating-linear-gradient(45deg, rgba(255,255,255,0.02), rgba(255,255,255,0.02) 6px, transparent 6px, transparent 12px)'
                      }}>
                        Ablated heuristic // Not evaluated
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            marginTop: '16px',
            fontSize: '10px',
            fontFamily: 'var(--font-mono)',
            color: 'var(--muted-text)',
            borderTop: '1px solid var(--contour-border)',
            paddingTop: '8px'
          }}>
            <span>Scale: 0.00</span>
            <span>0.20</span>
            <span>0.40</span>
            <span>0.60</span>
            <span>0.80</span>
          </div>
        </div>

        {/* Chart 2: STOP Recall vs ACT False-Alarm Rate (Safety Trade-off Pareto Frontier) */}
        <div className="card" style={{ padding: '20px', backgroundColor: 'var(--bg-secondary)' }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '16px',
            borderBottom: '1px solid var(--contour-border)',
            paddingBottom: '10px'
          }}>
            <div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-main)', fontFamily: 'var(--font-display)' }}>
                STOP Recall vs. ACT False-Alert Rate
              </div>
              <div style={{ fontSize: '11px', color: 'var(--muted-text)', fontFamily: 'var(--font-mono)' }}>
                Trade-off frontier: High STOP recall (↑) with minimal false alarms (←)
              </div>
            </div>
            <span style={{ fontSize: '11px', color: 'var(--signal-lime)', fontFamily: 'var(--font-mono)' }}>
              TARGET: TOP-LEFT QUADRANT
            </span>
          </div>

          {/* 2D Trade-off Scatter Plot */}
          <div style={{ position: 'relative', height: '210px', backgroundColor: '#07090f', borderRadius: '6px', border: '1px solid var(--contour-border)', padding: '16px' }}>
            {/* Axis gridlines */}
            <div style={{ position: 'absolute', left: '10%', right: '5%', top: '50%', height: '1px', borderTop: '1px dashed rgba(255,255,255,0.08)' }} />
            <div style={{ position: 'absolute', left: '50%', top: '10%', bottom: '15%', width: '1px', borderLeft: '1px dashed rgba(255,255,255,0.08)' }} />

            {/* Target safety zone (top left) */}
            <div style={{
              position: 'absolute',
              left: '12%',
              top: '12%',
              width: '25%',
              height: '35%',
              backgroundColor: 'rgba(216, 255, 99, 0.04)',
              border: '1px solid rgba(216, 255, 99, 0.15)',
              borderRadius: '4px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '9px',
              fontFamily: 'var(--font-mono)',
              color: 'var(--signal-lime)'
            }}>
              PARETO OPTIMAL
            </div>

            {/* Points for B1, B3, B4, B5, B6 */}
            {[
              { id: 'B1', name: 'Artifact-Only', stop: 0.90, far: 0.95, color: '#f87171' },
              { id: 'B3', name: 'Multimodal (No Intent)', stop: 0.40, far: 1.00, color: '#fb923c' },
              { id: 'B4', name: 'Fixed Policy (λ=0)', stop: 0.95, far: 0.10, color: '#45e4ff' },
              { id: 'B5', name: 'ContextGuard (Oracle)', stop: 0.95, far: 0.10, color: '#d8ff63', highlight: true },
              { id: 'B6', name: 'ContextGuard (Inferred)', stop: 0.95, far: 0.50, color: '#8b70ff' },
            ].map((pt) => {
              // Map FAR (0 to 1) to X (12% to 92%)
              const leftPercent = 12 + pt.far * 76;
              // Map STOP Recall (0 to 1) to Y (82% down to 14%)
              const topPercent = 82 - pt.stop * 68;

              return (
                <div
                  key={pt.id}
                  title={`${pt.id}: STOP Recall ${(pt.stop * 100).toFixed(0)}%, ACT False Alarms ${(pt.far * 100).toFixed(0)}%`}
                  style={{
                    position: 'absolute',
                    left: `${leftPercent}%`,
                    top: `${topPercent}%`,
                    transform: 'translate(-50%, -50%)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    cursor: 'pointer'
                  }}
                >
                  <div style={{
                    width: pt.highlight ? '14px' : '10px',
                    height: pt.highlight ? '14px' : '10px',
                    borderRadius: '50%',
                    backgroundColor: pt.color,
                    boxShadow: pt.highlight ? `0 0 12px ${pt.color}` : 'none',
                    border: '2px solid #07090f'
                  }} />
                  <span style={{
                    fontSize: '10px',
                    fontFamily: 'var(--font-mono)',
                    fontWeight: pt.highlight ? 700 : 500,
                    color: pt.highlight ? '#ffffff' : '#cbd5e1',
                    backgroundColor: 'rgba(0, 0, 0, 0.75)',
                    padding: '1px 5px',
                    borderRadius: '3px',
                    whiteSpace: 'nowrap'
                  }}>
                    {pt.id}
                  </span>
                </div>
              );
            })}

            {/* Axis labels */}
            <div style={{ position: 'absolute', left: '12px', top: '12px', fontSize: '10px', color: 'var(--muted-text)', fontFamily: 'var(--font-mono)' }}>
              ↑ STOP Recall (100%)
            </div>
            <div style={{ position: 'absolute', right: '14px', bottom: '10px', fontSize: '10px', color: 'var(--muted-text)', fontFamily: 'var(--font-mono)' }}>
              ACT False Alarm Rate (100%) →
            </div>
            <div style={{ position: 'absolute', left: '12px', bottom: '10px', fontSize: '10px', color: 'var(--muted-text)', fontFamily: 'var(--font-mono)' }}>
              FAR: 0%
            </div>
          </div>

          <div style={{ fontSize: '11px', color: 'var(--muted-text)', marginTop: '12px', lineHeight: 1.4 }}>
            <strong style={{ color: 'var(--signal-lime)' }}>Key Finding:</strong> Artifact-only B1 flags 95% of benign actions as hazardous (severe alert fatigue). ContextGuard reduces false alarms to 10% while retaining 95% catastrophic STOP recall.
          </div>
        </div>
      </div>

      {/* Distinctive Visual: 4x4 Intervention Confusion Matrix for ContextGuard B5 */}
      <div className="card" style={{ padding: '22px', backgroundColor: 'var(--bg-secondary)', marginBottom: '24px' }}>
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderBottom: '1px solid var(--contour-border)',
          paddingBottom: '12px',
          marginBottom: '16px'
        }}>
          <div>
            <div style={{
              fontSize: '11px',
              fontFamily: 'var(--font-mono)',
              color: 'var(--electric-violet)',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              marginBottom: '2px'
            }}>
              4×4 Policy Resolution Tensor // EARB B5 Run
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
              <Grid size={18} color="var(--electric-violet)" />
              Intervention Confusion Matrix: Predicted vs Expected Interventions
            </h3>
          </div>
          <div style={{
            fontSize: '11px',
            fontFamily: 'var(--font-mono)',
            color: 'var(--text-main)',
            backgroundColor: 'rgba(255, 255, 255, 0.03)',
            padding: '4px 10px',
            borderRadius: '4px',
            border: '1px solid var(--contour-border)'
          }}>
            TOTAL PAIRS: 60 // ZERO UNACCOUNTED
          </div>
        </div>

        {/* 4x4 Grid Render */}
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: '6px', textAlign: 'center' }}>
            <thead>
              <tr>
                <th style={{ textAlign: 'left', width: '140px', fontSize: '11px', color: 'var(--muted-text)', fontFamily: 'var(--font-mono)' }}>
                  EXPECTED \ PREDICTED
                </th>
                {confusionLabels.map((lbl) => (
                  <th key={lbl} style={{
                    padding: '8px',
                    fontSize: '12px',
                    fontFamily: 'var(--font-mono)',
                    color: lbl === 'STOP' ? 'var(--stop)' : lbl === 'WARN' ? 'var(--warn)' : lbl === 'ASK' ? 'var(--ask)' : 'var(--act)',
                    fontWeight: 700,
                    backgroundColor: 'rgba(255, 255, 255, 0.02)',
                    borderRadius: '4px',
                    border: '1px solid var(--contour-border)'
                  }}>
                    PRED: {lbl}
                  </th>
                ))}
                <th style={{ fontSize: '11px', color: 'var(--muted-text)', fontFamily: 'var(--font-mono)' }}>
                  CLASS RECALL
                </th>
              </tr>
            </thead>
            <tbody>
              {confusionLabels.map((expLbl) => {
                const row = confusionData[expLbl];
                const recall = ((row[expLbl] / row.total) * 100).toFixed(1);

                return (
                  <tr key={expLbl}>
                    <td style={{
                      textAlign: 'left',
                      padding: '10px 12px',
                      fontSize: '12px',
                      fontFamily: 'var(--font-mono)',
                      fontWeight: 700,
                      color: expLbl === 'STOP' ? 'var(--stop)' : expLbl === 'WARN' ? 'var(--warn)' : expLbl === 'ASK' ? 'var(--ask)' : 'var(--act)',
                      backgroundColor: 'rgba(255, 255, 255, 0.02)',
                      borderRadius: '4px',
                      border: '1px solid var(--contour-border)'
                    }}>
                      EXP: {expLbl} (n={row.total})
                    </td>

                    {confusionLabels.map((predLbl) => {
                      const count = row[predLbl];
                      const isDiagonal = expLbl === predLbl;
                      const opacity = isDiagonal ? Math.max(0.2, (count / row.total) * 0.8) : count > 0 ? 0.15 : 0.03;

                      let cellColor = 'var(--text-main)';
                      if (isDiagonal) {
                        cellColor = expLbl === 'STOP' ? 'var(--stop)' : expLbl === 'WARN' ? 'var(--warn)' : expLbl === 'ASK' ? 'var(--ask)' : 'var(--act)';
                      } else if (count > 0) {
                        cellColor = '#fca5a5';
                      }

                      return (
                        <td
                          key={predLbl}
                          onMouseEnter={() => setActiveMatrixCell({ exp: expLbl, pred: predLbl, val: count })}
                          onMouseLeave={() => setActiveMatrixCell(null)}
                          style={{
                            padding: '14px',
                            backgroundColor: isDiagonal
                              ? `rgba(69, 228, 255, ${opacity})`
                              : count > 0
                              ? `rgba(239, 68, 68, ${opacity})`
                              : 'rgba(255, 255, 255, 0.02)',
                            borderRadius: '6px',
                            border: isDiagonal
                              ? '1px solid rgba(69, 228, 255, 0.4)'
                              : '1px solid var(--contour-border)',
                            fontFamily: 'var(--font-mono)',
                            cursor: 'pointer',
                            transition: 'all 0.15s ease'
                          }}
                        >
                          <div style={{ fontSize: '18px', fontWeight: 700, color: cellColor }}>
                            {count}
                          </div>
                          <div style={{ fontSize: '10px', color: 'var(--muted-text)', marginTop: '2px' }}>
                            {((count / row.total) * 100).toFixed(0)}%
                          </div>
                        </td>
                      );
                    })}

                    <td style={{
                      padding: '10px',
                      fontSize: '12px',
                      fontFamily: 'var(--font-mono)',
                      fontWeight: 600,
                      color: Number(recall) >= 80 ? 'var(--signal-lime)' : 'var(--text-main)',
                      backgroundColor: 'rgba(255, 255, 255, 0.02)',
                      borderRadius: '4px',
                      border: '1px solid var(--contour-border)'
                    }}>
                      {recall}%
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Matrix Inspection Detail */}
        <div style={{
          marginTop: '12px',
          padding: '8px 12px',
          backgroundColor: '#07090f',
          borderRadius: '6px',
          border: '1px solid var(--contour-border)',
          display: 'flex',
          justifyContent: 'space-between',
          fontSize: '11px',
          fontFamily: 'var(--font-mono)',
          color: 'var(--muted-text)'
        }}>
          <span>
            {activeMatrixCell ? (
              <>
                <strong style={{ color: 'var(--ion-cyan)' }}>Cell Inspect:</strong> Expected {activeMatrixCell.exp} → Predicted {activeMatrixCell.pred}: {activeMatrixCell.val} pairs.
              </>
            ) : (
              'Hover over any cell in the 4×4 tensor to inspect classification counts.'
            )}
          </span>
          <span style={{ color: 'var(--signal-lime)' }}>
            STOP Recall: 19/20 (95.0%) // ACT Recall: 18/20 (90.0%)
          </span>
        </div>
      </div>

      {/* Comprehensive Empirical Metric Table */}
      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Baseline System</th>
              <th>Status</th>
              <th>Accuracy</th>
              <th>Macro F1 [95% CI]</th>
              <th>STOP Recall</th>
              <th>ACT FAR (False Alarms)</th>
              <th>ECE</th>
              <th>Mean Latency</th>
            </tr>
          </thead>
          <tbody>
            {baselineRows.map((row) => {
              const isEvaluated = row.evaluated;

              return (
                <tr key={row.key}>
                  <td style={{ minWidth: '220px' }}>
                    <div style={{ fontWeight: 600, color: isEvaluated ? 'var(--text-main)' : 'var(--muted-text)' }}>
                      {row.name}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--muted-text)', marginTop: '2px' }}>
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
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {isEvaluated && row.accuracy !== null ? (
                      `${(row.accuracy * 100).toFixed(1)}%`
                    ) : (
                      <span className="badge badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                    {isEvaluated && row.macroF1 !== null ? (
                      <div>
                        <span style={{ color: row.macroF1 >= 0.5 ? 'var(--signal-lime)' : 'var(--text-main)' }}>
                          {row.macroF1.toFixed(4)}
                        </span>
                        {row.ci && (
                          <div style={{ fontSize: '10px', color: 'var(--muted-text)' }}>
                            [{row.ci[0].toFixed(3)}, {row.ci[1].toFixed(3)}]
                          </div>
                        )}
                      </div>
                    ) : (
                      <span className="badge badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {isEvaluated && row.stopRecall !== null ? (
                      <span style={{ color: row.stopRecall >= 0.9 ? 'var(--signal-lime)' : 'var(--warn)' }}>
                        {(row.stopRecall * 100).toFixed(1)}%
                      </span>
                    ) : (
                      <span className="badge badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {isEvaluated && row.actFar !== null ? (
                      <span style={{ color: row.actFar <= 0.15 ? 'var(--signal-lime)' : '#f87171' }}>
                        {(row.actFar * 100).toFixed(1)}%
                      </span>
                    ) : (
                      <span className="badge badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {isEvaluated && row.ece !== null ? (
                      row.ece.toFixed(4)
                    ) : (
                      <span className="badge badge-not-eval">Not evaluated</span>
                    )}
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {isEvaluated && row.latency !== null ? (
                      `${row.latency.toFixed(2)} ms`
                    ) : (
                      <span className="badge badge-not-eval">Not evaluated</span>
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
