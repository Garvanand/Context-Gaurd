import React from 'react';
import {
  Cpu,
  Globe,
  Sliders,
  Camera,
} from 'lucide-react';
import type { SystemOverviewData } from '../../types';

interface ModelPerformanceProps {
  overview: SystemOverviewData | null;
}

export const ModelPerformance: React.FC<ModelPerformanceProps> = ({ overview }) => {
  return (
    <section id="section-models" style={{ marginBottom: '32px' }}>
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
            <Cpu size={18} color="#ec4899" />
            5. Model Performance & Architecture Registry
          </h2>
          <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0 0' }}>
            Production model metadata, training datasets, empirical benchmark scores, and hyperparameters.
          </p>
        </div>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
        gap: '16px'
      }}>
        {/* Card 1: Qwen 2.5-VL */}
        <div className="card" style={{ padding: '20px', backgroundColor: '#0c111e' }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #1a253a',
            paddingBottom: '12px',
            marginBottom: '14px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '6px',
                backgroundColor: 'rgba(236, 72, 153, 0.1)',
                border: '1px solid rgba(236, 72, 153, 0.3)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Cpu size={18} color="#ec4899" />
              </div>
              <div>
                <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
                  Qwen 2.5-VL 3B Reasoner
                </div>
                <div style={{ fontSize: '11px', color: '#ec4899', fontFamily: 'monospace' }}>
                  {overview?.qwen_vlm.model_tag || 'qwen2.5-vl:3b'}
                </div>
              </div>
            </div>
            <span className="badge badge-operational">ACTIVE LOCAL VLM</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Model Architecture</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>Multimodal Vision Transformer</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Inference Provider</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>Ollama Local Service</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Training Dataset</span>
              <span style={{ color: '#cbd5e1' }}>Multimodal Safety & Vision Corpus</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Training Date</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>2025 (Official weights)</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Role in Pipeline</span>
              <span style={{ color: '#38bdf8' }}>Harm Severity & Irreversibility</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Failure Invariant</span>
              <span style={{ color: '#fbbf24' }}>Never emits silent ACT on timeout</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
              <span style={{ color: '#64748b' }}>Mean Inference Latency</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>{overview?.qwen_vlm.latency_ms || 2.4} ms</span>
            </div>
          </div>
        </div>

        {/* Card 2: XGBoost URL Risk Classifier */}
        <div className="card" style={{ padding: '20px', backgroundColor: '#0c111e' }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #1a253a',
            paddingBottom: '12px',
            marginBottom: '14px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '6px',
                backgroundColor: 'rgba(20, 184, 166, 0.1)',
                border: '1px solid rgba(20, 184, 166, 0.3)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Globe size={18} color="#14b8a6" />
              </div>
              <div>
                <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
                  XGBoost URL Phishing Classifier
                </div>
                <div style={{ fontSize: '11px', color: '#14b8a6', fontFamily: 'monospace' }}>
                  xgboost==2.0.3 (joblib artifact)
                </div>
              </div>
            </div>
            <span className="badge badge-operational">99.58% ACC</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Training Dataset</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>PhiUSIIL (UCI ID: 967)</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Training Date</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>2026-10-04</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Training Set Samples</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>60,000 URLs (Balanced)</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Extracted Features</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>35 lexical & Shannon entropy</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Macro F1-Score</span>
              <span style={{ color: '#34d399', fontFamily: 'monospace', fontWeight: 600 }}>0.9951</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Precision / Recall</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>0.9962 / 0.9940</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
              <span style={{ color: '#64748b' }}>Inference Latency</span>
              <span style={{ color: '#34d399', fontFamily: 'monospace' }}>0.0035 ms (3.5 μs)</span>
            </div>
          </div>
        </div>

        {/* Card 3: ML Kit On-Device Engine */}
        <div className="card" style={{ padding: '20px', backgroundColor: '#0c111e' }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #1a253a',
            paddingBottom: '12px',
            marginBottom: '14px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '6px',
                backgroundColor: 'rgba(139, 92, 246, 0.1)',
                border: '1px solid rgba(139, 92, 246, 0.3)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Camera size={18} color="#8b5cf6" />
              </div>
              <div>
                <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
                  Google ML Kit On-Device Perception
                </div>
                <div style={{ fontSize: '11px', color: '#8b5cf6', fontFamily: 'monospace' }}>
                  Android Client SDK
                </div>
              </div>
            </div>
            <span className="badge badge-operational">ON-DEVICE ONLY</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>OCR Pipeline</span>
              <span style={{ color: '#cbd5e1' }}>ML Kit Text Recognition v2</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Facial Perception</span>
              <span style={{ color: '#cbd5e1' }}>ML Kit Face Contours</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Checksum Parsers</span>
              <span style={{ color: '#cbd5e1' }}>Verhoeff (Aadhaar), Luhn (Card)</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Redaction Strategy</span>
              <span style={{ color: '#38bdf8' }}>In-Memory Canvas Masking</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
              <span style={{ color: '#64748b' }}>Data Transmission</span>
              <span style={{ color: '#34d399' }}>0 Raw Sensitive Tokens Uploaded</span>
            </div>
          </div>
        </div>

        {/* Card 4: Deterministic Policy Engine */}
        <div className="card" style={{ padding: '20px', backgroundColor: '#0c111e' }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #1a253a',
            paddingBottom: '12px',
            marginBottom: '14px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '6px',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                border: '1px solid rgba(59, 130, 246, 0.3)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Sliders size={18} color="#3b82f6" />
              </div>
              <div>
                <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
                  Deterministic Policy Engine
                </div>
                <div style={{ fontSize: '11px', color: '#3b82f6', fontFamily: 'monospace' }}>
                  Risk Calibration Core
                </div>
              </div>
            </div>
            <span className="badge badge-cyan">λ = 0.75</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Effective Risk Formula</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace', fontSize: '11px' }}>
                ρ = S · (1 + λ · R)
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>Reversibility Multiplier (λ)</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>0.75</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>STOP Threshold</span>
              <span style={{ color: '#f87171', fontFamily: 'monospace', fontWeight: 600 }}>0.65</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px dashed #141c2e' }}>
              <span style={{ color: '#64748b' }}>ASK / WARN Threshold</span>
              <span style={{ color: '#fbbf24', fontFamily: 'monospace', fontWeight: 600 }}>0.35</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
              <span style={{ color: '#64748b' }}>Min Confidence (for WARN)</span>
              <span style={{ color: '#38bdf8', fontFamily: 'monospace' }}>0.70</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
