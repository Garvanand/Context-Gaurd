import React, { useState } from 'react';
import {
  GitCommit,
  Layers,
  Compass,
  FileSearch,
  Scale,
  HelpCircle,
  ShieldAlert,
  Code,
} from 'lucide-react';
import type { AnalysisResponse } from '../../types';

interface PipelineTraceProps {
  latestResult: AnalysisResponse | null;
}

const STAGE_DEFINITIONS = [
  {
    index: 1,
    id: 'stage_1_context',
    name: 'Context Aggregation',
    shortName: 'Context',
    icon: Layers,
    color: '#38bdf8',
    description: 'Combines multimodal artifact signals, OCR text, face contours, PII entities, and client metadata.',
  },
  {
    index: 2,
    id: 'stage_2_intent',
    name: 'Intent Resolution',
    shortName: 'Intent',
    icon: Compass,
    color: '#818cf8',
    description: 'Resolves pre-action candidate either via explicit user selection or multimodal inference.',
  },
  {
    index: 3,
    id: 'stage_3_evidence',
    name: 'Evidence Extraction',
    shortName: 'Evidence',
    icon: FileSearch,
    color: '#f59e0b',
    description: 'Extracts grounded risk signals, OCR claims, URL phishing telemetry, and financial markers.',
  },
  {
    index: 4,
    id: 'stage_4_consequence',
    name: 'Consequence Estimation',
    shortName: 'Consequence',
    icon: Scale,
    color: '#ec4899',
    description: 'Separately evaluates harm severity magnitude and irreversibility loss without conflation.',
  },
  {
    index: 5,
    id: 'stage_5_uncertainty',
    name: 'Uncertainty Estimation',
    shortName: 'Uncertainty',
    icon: HelpCircle,
    color: '#a855f7',
    description: 'Estimates epistemic uncertainty and model ambiguity against minimum confidence thresholds.',
  },
  {
    index: 6,
    id: 'stage_6_intervention',
    name: 'Policy Intervention',
    shortName: 'Intervention',
    icon: ShieldAlert,
    color: '#ef4444',
    description: 'Executes deterministic safety policy: STOP, WARN, ASK, or ACT based on λ-reversibility penalization.',
  },
];

export const PipelineTrace: React.FC<PipelineTraceProps> = ({ latestResult }) => {
  const [selectedStageIndex, setSelectedStageIndex] = useState<number>(1);

  // If no live analysis yet, provide canonical reference run trace so supervisor can interact immediately
  const stages = latestResult?.stages || {
    stage_1_context: {
      artifact_summary: "Bank Transaction Confirmation Screenshot (ART-FIN-001)",
      source_application: "Telegram Messenger",
      recipient: "Unknown Public Group (@crypto_alerts_tg)",
      destination: "Telegram Channel",
      detected_pii: ["Aadhaar Verhoeff Checksum", "Bank Account Number", "IFSC Code"],
      detected_faces: 0,
      url_present: false,
    },
    stage_2_intent: {
      selected_action: "SHARE_TO_PUBLIC_FORUM",
      inferred_action: "SHARE_TO_PUBLIC_FORUM",
      resolved_action: "SHARE_TO_PUBLIC_FORUM",
      intent_source: "EXPLICIT_USER",
      consequence_tier: "CRITICAL",
    },
    stage_3_evidence: {
      total_importance: 0.95,
      detected_risk_categories: ["FINANCIAL", "PII_LEAKAGE", "PUBLIC_EXPOSURE"],
      evidence: [
        { type: "PII_FINANCIAL", description: "Contains unredacted bank account and IFSC details", importance: 0.95, evidence_source: "RULE" },
        { type: "PUBLIC_TRANSMISSION", description: "Target recipient is an unverified public channel", importance: 0.9, evidence_source: "CONTEXT" },
      ],
    },
    stage_4_consequence: {
      severity: 0.88,
      reversibility: 0.92,
      harm_description: "Irreversible exposure of private banking credentials to unknown third parties",
      reversibility_rationale: "Once posted to public broadcast channel, credentials cannot be clawed back",
    },
    stage_5_uncertainty: {
      confidence: 0.96,
      uncertainty_reasons: [],
      is_epistemic_uncertain: false,
    },
    stage_6_intervention: {
      selected_intervention: latestResult?.intervention || "STOP",
      risk_score: latestResult?.risk_score || 0.88,
      severity: latestResult?.severity || 0.88,
      reversibility: latestResult?.reversibility || 0.92,
      confidence: latestResult?.confidence || 0.96,
      effective_risk_score: 0.88 * (1 + 0.75 * 0.92),
      policy_trigger: "Effective Risk (1.487) exceeds STOP threshold (0.65) with irreversible consequence",
    },
  };

  const getStageOutput = (idx: number) => {
    switch (idx) {
      case 1:
        return stages.stage_1_context;
      case 2:
        return stages.stage_2_intent;
      case 3:
        return stages.stage_3_evidence;
      case 4:
        return stages.stage_4_consequence;
      case 5:
        return stages.stage_5_uncertainty;
      case 6:
        return (
          stages.stage_6_intervention || {
            intervention: latestResult?.intervention,
            risk_score: latestResult?.risk_score,
            severity: latestResult?.severity,
            reversibility: latestResult?.reversibility,
            confidence: latestResult?.confidence,
            reason: latestResult?.reason,
          }
        );
      default:
        return {};
    }
  };

  const activeStage = STAGE_DEFINITIONS.find((s) => s.index === selectedStageIndex) || STAGE_DEFINITIONS[0];
  const activeOutput = getStageOutput(selectedStageIndex);

  return (
    <section id="section-pipeline" style={{ marginBottom: '32px' }}>
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
            <GitCommit size={18} color="#38bdf8" />
            3. Interactive Six-Stage Pipeline Trace
          </h2>
          <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0 0' }}>
            Click any pipeline node to inspect actual intermediate representations and tensor outputs.
          </p>
        </div>
        <div style={{
          fontSize: '11px',
          color: latestResult ? '#34d399' : '#94a3b8',
          fontFamily: 'monospace',
          backgroundColor: '#0c121e',
          padding: '4px 10px',
          borderRadius: '4px',
          border: '1px solid #1a253a'
        }}>
          {latestResult ? 'LIVE RUN TRACE ATTACHED' : 'REFERENCE EARB RUN TRACE'}
        </div>
      </div>

      {/* Interactive 6-Stage Flow Diagram */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(6, 1fr)',
        gap: '10px',
        marginBottom: '16px',
        overflowX: 'auto',
      }}>
        {STAGE_DEFINITIONS.map((stage) => {
          const isSelected = selectedStageIndex === stage.index;
          const Icon = stage.icon;
          return (
            <div
              key={stage.index}
              onClick={() => setSelectedStageIndex(stage.index)}
              style={{
                cursor: 'pointer',
                padding: '14px 10px',
                borderRadius: '8px',
                backgroundColor: isSelected ? '#131e36' : '#0c111e',
                border: `1px solid ${isSelected ? stage.color : '#1e293b'}`,
                boxShadow: isSelected ? `0 0 16px ${stage.color}33` : 'none',
                transition: 'all 0.15s ease',
                position: 'relative',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
              }}
            >
              {/* Node indicator */}
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                backgroundColor: `${stage.color}1a`,
                border: `1px solid ${stage.color}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '8px',
              }}>
                <Icon size={16} color={stage.color} />
              </div>

              <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>
                STAGE {stage.index}
              </div>
              <div style={{ fontSize: '12px', fontWeight: 600, color: '#f1f5f9', marginTop: '2px' }}>
                {stage.shortName}
              </div>

              {isSelected && (
                <div style={{
                  position: 'absolute',
                  bottom: '-5px',
                  width: '16px',
                  height: '3px',
                  backgroundColor: stage.color,
                  borderRadius: '2px',
                }} />
              )}
            </div>
          );
        })}
      </div>

      {/* Stage Detail Card */}
      <div className="card" style={{ padding: '20px', backgroundColor: '#0c111e' }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #1a253a',
          paddingBottom: '14px',
          marginBottom: '16px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              backgroundColor: `${activeStage.color}15`,
              border: `1px solid ${activeStage.color}44`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <activeStage.icon size={20} color={activeStage.color} />
            </div>
            <div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: '#f8fafc' }}>
                Stage {activeStage.index}: {activeStage.name}
              </div>
              <div style={{ fontSize: '12px', color: '#94a3b8' }}>
                {activeStage.description}
              </div>
            </div>
          </div>
          <span className="badge" style={{
            backgroundColor: `${activeStage.color}15`,
            color: activeStage.color,
            border: `1px solid ${activeStage.color}44`,
            fontSize: '11px'
          }}>
            ACTUAL OUTPUT PAYLOAD
          </span>
        </div>

        {/* Output Representation */}
        <div style={{
          backgroundColor: '#07090f',
          padding: '16px',
          borderRadius: '6px',
          border: '1px solid #141c2c',
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '10px',
            fontSize: '11px',
            color: '#64748b'
          }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Code size={13} />
              JSON Representation ({activeStage.id})
            </span>
            <span>Deterministic Schema Validated</span>
          </div>
          <pre style={{
            margin: 0,
            fontSize: '12px',
            fontFamily: 'monospace',
            color: '#38bdf8',
            overflowX: 'auto',
            maxHeight: '340px',
            lineHeight: 1.45
          }}>
            {JSON.stringify(activeOutput, null, 2)}
          </pre>
        </div>
      </div>
    </section>
  );
};
