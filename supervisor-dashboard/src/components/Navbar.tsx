import React, { useState, useEffect } from 'react';
import { Shield, RefreshCw, Smartphone, Server, Lock } from 'lucide-react';
import type { SystemOverviewData } from '../types';

interface NavbarProps {
  overview: SystemOverviewData | null;
  loading: boolean;
  onRefresh: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ overview, loading, onRefresh }) => {
  const [timeStr, setTimeStr] = useState<string>('');

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTimeStr(now.toTimeString().split(' ')[0] + ' UTC' + (now.getTimezoneOffset() <= 0 ? '+' : '-') + Math.abs(Math.round(now.getTimezoneOffset() / 60)));
    };
    updateTime();
    const timer = setInterval(updateTime, 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <header style={{
      height: '64px',
      backgroundColor: '#090d16',
      borderBottom: '1px solid #1e293b',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px',
      position: 'sticky',
      top: 0,
      zIndex: 50,
      backdropFilter: 'blur(12px)',
    }}>
      {/* Brand & Title */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div style={{
          width: '38px',
          height: '38px',
          borderRadius: '8px',
          backgroundColor: '#1e293b',
          border: '1px solid #3b82f6',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 12px rgba(59, 130, 246, 0.35)',
        }}>
          <Shield size={22} color="#60a5fa" />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '16px', fontWeight: 700, letterSpacing: '0.02em', color: '#ffffff' }}>
              CONTEXTGUARD
            </span>
            <span style={{
              fontSize: '10px',
              fontFamily: 'monospace',
              padding: '2px 6px',
              borderRadius: '4px',
              backgroundColor: '#1e293b',
              color: '#38bdf8',
              border: '1px solid #0284c7',
            }}>
              SUPERVISOR v0.1.0
            </span>
          </div>
          <div style={{ fontSize: '11px', color: '#64748b', letterSpacing: '0.04em' }}>
            HIGH-ASSURANCE MULTIMODAL SAFETY CONTROL ROOM
          </div>
        </div>
      </div>

      {/* Telemetry Status Bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
        {/* Android Device Status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px' }}>
          <Smartphone size={14} color="#10b981" />
          <span style={{ color: '#94a3b8' }}>Android Client:</span>
          <span style={{
            color: '#34d399',
            fontFamily: 'monospace',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: '5px'
          }}>
            <span style={{
              width: '6px',
              height: '6px',
              borderRadius: '50%',
              backgroundColor: '#10b981',
              display: 'inline-block'
            }} />
            {overview?.android_connection?.status || 'CONNECTED'}
          </span>
        </div>

        {/* Backend Status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px' }}>
          <Server size={14} color="#38bdf8" />
          <span style={{ color: '#94a3b8' }}>Backend:</span>
          <span style={{
            color: '#38bdf8',
            fontFamily: 'monospace',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: '5px'
          }}>
            <span style={{
              width: '6px',
              height: '6px',
              borderRadius: '50%',
              backgroundColor: '#38bdf8',
              display: 'inline-block'
            }} />
            {overview?.backend?.status || 'HEALTHY'}
          </span>
        </div>

        {/* Zero Disk Invariant */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          fontSize: '11px',
          padding: '4px 10px',
          backgroundColor: '#0e1726',
          border: '1px solid #1e3a5f',
          borderRadius: '4px',
          color: '#38bdf8',
        }}>
          <Lock size={12} color="#38bdf8" />
          <span style={{ fontWeight: 500 }}>RAM-ONLY STORAGE</span>
        </div>

        {/* Live Clock */}
        <div style={{
          fontSize: '12px',
          fontFamily: 'monospace',
          color: '#cbd5e1',
          padding: '4px 8px',
          backgroundColor: '#0f172a',
          borderRadius: '4px',
          border: '1px solid #1e293b'
        }}>
          {timeStr}
        </div>

        {/* Manual Refresh */}
        <button
          onClick={onRefresh}
          disabled={loading}
          style={{
            padding: '6px 12px',
            backgroundColor: '#1e293b',
            border: '1px solid #334155',
            borderRadius: '6px',
            color: '#e2e8f0',
            fontSize: '12px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            cursor: loading ? 'not-allowed' : 'pointer',
            opacity: loading ? 0.7 : 1,
            transition: 'all 0.15s ease',
          }}
          title="Refresh Telemetry"
        >
          <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
          <span>Sync</span>
        </button>
      </div>
    </header>
  );
};
