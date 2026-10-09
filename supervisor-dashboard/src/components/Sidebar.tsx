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
} from 'lucide-react';

export type SectionId =
  | 'overview'
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
    <aside style={{
      width: '260px',
      backgroundColor: '#070a12',
      borderRight: '1px solid #1e293b',
      height: 'calc(100vh - 64px)',
      position: 'sticky',
      top: '64px',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
      padding: '16px 10px',
      overflowY: 'auto',
    }}>
      <div>
        <div style={{
          padding: '0 10px 12px 10px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #141d2e',
          marginBottom: '10px'
        }}>
          <span style={{
            fontSize: '11px',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            color: '#64748b'
          }}>
            CONSOLE PANELS
          </span>
          <button
            onClick={() => onSelectSection('all')}
            style={{
              fontSize: '11px',
              padding: '2px 8px',
              borderRadius: '4px',
              backgroundColor: activeSection === 'all' ? '#2563eb' : '#172033',
              color: activeSection === 'all' ? '#ffffff' : '#94a3b8',
              border: '1px solid ' + (activeSection === 'all' ? '#3b82f6' : '#1e293b'),
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px'
            }}
          >
            <Layers size={11} />
            <span>Full View</span>
          </button>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
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
                  borderRadius: '6px',
                  backgroundColor: isActive ? '#131e36' : 'transparent',
                  color: isActive ? '#60a5fa' : '#cbd5e1',
                  border: '1px solid ' + (isActive ? '#2563eb' : 'transparent'),
                  cursor: 'pointer',
                  textAlign: 'left',
                  fontSize: '12px',
                  fontWeight: isActive ? 600 : 500,
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.backgroundColor = '#0d1424';
                    e.currentTarget.style.color = '#f1f5f9';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.backgroundColor = 'transparent';
                    e.currentTarget.style.color = '#cbd5e1';
                  }
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Icon size={16} color={isActive ? '#38bdf8' : '#64748b'} />
                  <span>{sec.label}</span>
                </div>
                {sec.badge && (
                  <span style={{
                    fontSize: '9px',
                    fontFamily: 'monospace',
                    padding: '2px 5px',
                    borderRadius: '3px',
                    backgroundColor: isActive ? 'rgba(56, 189, 248, 0.2)' : '#162032',
                    color: isActive ? '#38bdf8' : '#94a3b8',
                    border: '1px solid ' + (isActive ? 'rgba(56, 189, 248, 0.4)' : '#1e293b'),
                  }}>
                    {sec.badge}
                  </span>
                )}
                {sec.id === 'failures' && failureCount > 0 && !sec.badge && (
                  <span style={{
                    fontSize: '10px',
                    fontWeight: 700,
                    padding: '1px 6px',
                    borderRadius: '10px',
                    backgroundColor: 'rgba(239, 68, 68, 0.2)',
                    color: '#f87171',
                    border: '1px solid rgba(239, 68, 68, 0.4)'
                  }}>
                    {failureCount}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Footer System Specs */}
      <div style={{
        padding: '12px',
        backgroundColor: '#0c121e',
        borderRadius: '6px',
        border: '1px solid #1a253a',
        fontSize: '11px',
      }}>
        <div style={{ color: '#64748b', marginBottom: '4px', fontWeight: 600 }}>SAFETY BOUNDARY</div>
        <div style={{ color: '#38bdf8', fontFamily: 'monospace', fontSize: '10px' }}>
          • Pre-action Interception<br/>
          • Reversibility Penalty λ=0.75<br/>
          • Stop Thresh: 0.65 | Ask: 0.35
        </div>
      </div>
    </aside>
  );
};
