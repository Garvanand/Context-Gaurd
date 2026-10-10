import React from 'react';
import {
  Activity,
  Zap,
  GitCommit,
  Database,
  Cpu,
  BarChart3,
  Sliders,
  ShieldCheck,
  AlertTriangle,
  Wifi,
  HeartPulse,
  Layers,
  Smartphone,
} from 'lucide-react';

export type SectionId =
  | 'overview'
  | 'relay'
  | 'live-analysis'
  | 'pipeline'
  | 'earb'
  | 'models'
  | 'baselines'
  | 'ablations'
  | 'privacy-utility'
  | 'failures'
  | 'network'
  | 'health'
  | 'all';

interface SidebarProps {
  activeSection: SectionId;
  onSelectSection: (id: SectionId) => void;
  failureCount: number;
}

export const SECTIONS: { id: SectionId; label: string; icon: React.FC<any>; badge?: string }[] = [
  { id: 'overview', label: '1. System Overview', icon: Activity },
  { id: 'relay', label: 'Live Mobile Relay', icon: Smartphone, badge: 'REALTIME' },
  { id: 'live-analysis', label: '2. Live Analysis', icon: Zap, badge: 'EXEC' },
  { id: 'pipeline', label: '3. Pipeline Trace', icon: GitCommit, badge: '6-STAGE' },
  { id: 'earb', label: '4. EARB Benchmark', icon: Database, badge: '60 PAIRS' },
  { id: 'models', label: '5. Model Performance', icon: Cpu },
  { id: 'baselines', label: '6. Baseline Comparison', icon: BarChart3 },
  { id: 'ablations', label: '7. Ablation Results', icon: Sliders },
  { id: 'privacy-utility', label: '8. Privacy-Utility', icon: ShieldCheck, badge: '3-MODE' },
  { id: 'failures', label: '9. Failure Analysis', icon: AlertTriangle, badge: 'AUDIT' },
  { id: 'network', label: '10. Network Activity', icon: Wifi },
  { id: 'health', label: '11. Model Health', icon: HeartPulse, badge: 'LIVE' },
];

export const Sidebar: React.FC<SidebarProps> = ({
  activeSection,
  onSelectSection,
  failureCount,
}) => {
  return (
    <aside
      style={{
        width: '260px',
        backgroundColor: 'var(--ink)',
        borderRight: '1px solid var(--contour-border)',
        height: 'calc(100vh - 68px)',
        position: 'sticky',
        top: '68px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        padding: '16px 12px',
        overflowY: 'auto',
      }}
    >
      <div>
        <div
          style={{
            padding: '0 8px 12px 8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid var(--contour-border)',
            marginBottom: '10px',
          }}
        >
          <span
            style={{
              fontSize: '11px',
              fontFamily: "'JetBrains Mono', monospace",
              fontWeight: 700,
              textTransform: 'uppercase',
              letterSpacing: '0.08em',
              color: 'var(--muted-text)',
            }}
          >
            PANELS
          </span>
          <button
            onClick={() => onSelectSection('all')}
            style={{
              fontSize: '11px',
              fontFamily: "'Inter', sans-serif",
              padding: '3px 9px',
              borderRadius: '6px',
              backgroundColor: activeSection === 'all' ? 'var(--electric-violet)' : 'var(--deep-surface)',
              color: activeSection === 'all' ? '#FFFFFF' : 'var(--muted-text)',
              border: '1px solid ' + (activeSection === 'all' ? 'var(--electric-violet)' : 'var(--contour-border)'),
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '5px',
              fontWeight: 500,
              transition: 'all 0.15s ease',
            }}
          >
            <Layers size={11} />
            <span>Full View</span>
          </button>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
          {SECTIONS.map((sec) => {
            const Icon = sec.icon;
            const isActive = activeSection === sec.id;
            return (
              <button
                key={sec.id}
                onClick={() => onSelectSection(sec.id)}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '9px 12px',
                  borderRadius: '8px',
                  backgroundColor: isActive ? 'rgba(139, 112, 255, 0.14)' : 'transparent',
                  color: isActive ? 'var(--text-main)' : 'var(--muted-text)',
                  border: '1px solid ' + (isActive ? 'var(--contour-border-active)' : 'transparent'),
                  cursor: 'pointer',
                  textAlign: 'left',
                  fontSize: '12px',
                  fontFamily: "'Inter', sans-serif",
                  fontWeight: isActive ? 600 : 500,
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.backgroundColor = 'var(--elevated-surface)';
                    e.currentTarget.style.color = 'var(--text-main)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.backgroundColor = 'transparent';
                    e.currentTarget.style.color = 'var(--muted-text)';
                  }
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Icon size={16} color={isActive ? 'var(--electric-violet)' : 'var(--muted-text)'} />
                  <span>{sec.label}</span>
                </div>
                {sec.badge && (
                  <span
                    style={{
                      fontSize: '9px',
                      fontFamily: "'JetBrains Mono', monospace",
                      fontWeight: 600,
                      padding: '2px 6px',
                      borderRadius: '4px',
                      backgroundColor: isActive ? 'rgba(69, 228, 255, 0.2)' : 'var(--deep-surface)',
                      color: isActive ? 'var(--ion-cyan)' : 'var(--muted-text)',
                      border: '1px solid ' + (isActive ? 'rgba(69, 228, 255, 0.4)' : 'var(--contour-border)'),
                    }}
                  >
                    {sec.badge}
                  </span>
                )}
                {sec.id === 'failures' && failureCount > 0 && !sec.badge && (
                  <span
                    style={{
                      fontSize: '10px',
                      fontWeight: 700,
                      padding: '1px 6px',
                      borderRadius: '10px',
                      backgroundColor: 'rgba(255, 102, 125, 0.2)',
                      color: 'var(--stop)',
                      border: '1px solid rgba(255, 102, 125, 0.4)',
                    }}
                  >
                    {failureCount}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Footer System Specs */}
      <div
        style={{
          padding: '12px',
          backgroundColor: 'var(--deep-surface)',
          borderRadius: '8px',
          border: '1px solid var(--contour-border)',
          fontSize: '11px',
        }}
      >
        <div
          style={{
            color: 'var(--muted-text)',
            marginBottom: '4px',
            fontWeight: 700,
            fontSize: '10px',
            fontFamily: "'JetBrains Mono', monospace",
            letterSpacing: '0.06em',
          }}
        >
          SPECTRAL SIGNAL BOUNDS
        </div>
        <div style={{ color: 'var(--ion-cyan)', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', lineHeight: '16px' }}>
          • Pre-action Interception<br />
          • Reversibility Penalty λ=0.75<br />
          • Stop: ≥0.65 | Ask: c&lt;0.70 &amp; ρ≥0.35
        </div>
      </div>
    </aside>
  );
};
