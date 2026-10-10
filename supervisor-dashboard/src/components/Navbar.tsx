import React, { useState, useEffect } from 'react';
import { RefreshCw, Smartphone, Server, Lock, Sun, Moon } from 'lucide-react';
import { ApertureSignalLogo } from './ApertureSignalLogo';
import type { SystemOverviewData } from '../types';

interface NavbarProps {
  overview: SystemOverviewData | null;
  loading: boolean;
  onRefresh: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ overview, loading, onRefresh }) => {
  const [timeStr, setTimeStr] = useState<string>('');
  const [isLightMode, setIsLightMode] = useState<boolean>(() => {
    return document.documentElement.getAttribute('data-theme') === 'light';
  });

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTimeStr(
        now.toTimeString().split(' ')[0] +
          ' UTC' +
          (now.getTimezoneOffset() <= 0 ? '+' : '-') +
          Math.abs(Math.round(now.getTimezoneOffset() / 60))
      );
    };
    updateTime();
    const timer = setInterval(updateTime, 1000);
    return () => clearInterval(timer);
  }, []);

  const toggleTheme = () => {
    const nextLight = !isLightMode;
    setIsLightMode(nextLight);
    if (nextLight) {
      document.documentElement.setAttribute('data-theme', 'light');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
  };

  return (
    <header
      style={{
        height: '68px',
        backgroundColor: 'var(--midnight)',
        borderBottom: '1px solid var(--contour-border)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 24px',
        position: 'sticky',
        top: 0,
        zIndex: 50,
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
      }}
    >
      {/* Brand & Signature Context Field Motif */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div
          style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            backgroundColor: 'var(--deep-surface)',
            border: '1px solid var(--contour-border-active)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 16px rgba(139, 112, 255, 0.25)',
          }}
        >
          <ApertureSignalLogo size={26} animated />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span
              style={{
                fontSize: '17px',
                fontFamily: "'Space Grotesk', sans-serif",
                fontWeight: 700,
                letterSpacing: '-0.02em',
                color: 'var(--text-main)',
              }}
            >
              CONTEXTGUARD
            </span>
            <span
              style={{
                fontSize: '10px',
                fontFamily: "'JetBrains Mono', monospace",
                padding: '2px 7px',
                borderRadius: '5px',
                backgroundColor: 'rgba(139, 112, 255, 0.15)',
                color: 'var(--electric-violet)',
                border: '1px solid var(--contour-border-active)',
                fontWeight: 600,
              }}
            >
              SPECTRAL SIGNAL
            </span>
          </div>
          <div
            style={{
              fontSize: '11px',
              fontFamily: "'Inter', sans-serif",
              color: 'var(--muted-text)',
              letterSpacing: '0.02em',
            }}
          >
            HIGH-ASSURANCE MULTIMODAL SAFETY CONTROL ROOM
          </div>
        </div>
      </div>

      {/* Telemetry Status Bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {/* Android Device Status */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '12px',
            padding: '5px 10px',
            backgroundColor: 'var(--deep-surface)',
            borderRadius: '6px',
            border: '1px solid var(--contour-border)',
          }}
        >
          <Smartphone size={14} color="var(--act)" />
          <span style={{ color: 'var(--muted-text)' }}>Android Sentinel:</span>
          <span
            style={{
              color: 'var(--act)',
              fontFamily: "'JetBrains Mono', monospace",
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <span
              style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                backgroundColor: 'var(--act)',
                display: 'inline-block',
                boxShadow: '0 0 6px var(--act)',
              }}
            />
            {overview?.android_connection?.status || 'CONNECTED'}
          </span>
        </div>

        {/* Backend Status */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '12px',
            padding: '5px 10px',
            backgroundColor: 'var(--deep-surface)',
            borderRadius: '6px',
            border: '1px solid var(--contour-border)',
          }}
        >
          <Server size={14} color="var(--ion-cyan)" />
          <span style={{ color: 'var(--muted-text)' }}>Inference:</span>
          <span
            style={{
              color: 'var(--ion-cyan)',
              fontFamily: "'JetBrains Mono', monospace",
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <span
              style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                backgroundColor: 'var(--ion-cyan)',
                display: 'inline-block',
                boxShadow: '0 0 6px var(--ion-cyan)',
              }}
            />
            {overview?.backend?.status || 'HEALTHY'}
          </span>
        </div>

        {/* Zero Disk Invariant */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '11px',
            padding: '5px 10px',
            backgroundColor: 'rgba(139, 112, 255, 0.12)',
            border: '1px solid var(--contour-border-active)',
            borderRadius: '6px',
            color: 'var(--electric-violet)',
            fontFamily: "'JetBrains Mono', monospace",
            fontWeight: 600,
          }}
        >
          <Lock size={12} color="var(--electric-violet)" />
          <span>RAM-ONLY STORAGE</span>
        </div>

        {/* Live Clock */}
        <div
          style={{
            fontSize: '11px',
            fontFamily: "'JetBrains Mono', monospace",
            color: 'var(--muted-text)',
            padding: '5px 10px',
            backgroundColor: 'var(--deep-surface)',
            borderRadius: '6px',
            border: '1px solid var(--contour-border)',
          }}
        >
          {timeStr}
        </div>

        {/* Theme Toggle (Dark Cinematic <-> Cool Porcelain) */}
        <button
          onClick={toggleTheme}
          style={{
            padding: '7px 11px',
            backgroundColor: 'var(--deep-surface)',
            border: '1px solid var(--contour-border)',
            borderRadius: '7px',
            color: 'var(--text-main)',
            fontSize: '12px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            cursor: 'pointer',
            transition: 'all 0.15s ease',
          }}
          title={isLightMode ? 'Switch to Dark Cinematic' : 'Switch to Cool Porcelain'}
        >
          {isLightMode ? <Moon size={13} color="var(--electric-violet)" /> : <Sun size={13} color="var(--signal-lime)" />}
          <span style={{ fontSize: '11px', fontWeight: 500 }}>
            {isLightMode ? 'Dark' : 'Porcelain'}
          </span>
        </button>

        {/* Manual Refresh */}
        <button
          onClick={onRefresh}
          disabled={loading}
          style={{
            padding: '7px 14px',
            backgroundColor: 'var(--electric-violet)',
            border: 'none',
            borderRadius: '7px',
            color: '#FFFFFF',
            fontSize: '12px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            cursor: loading ? 'not-allowed' : 'pointer',
            opacity: loading ? 0.7 : 1,
            boxShadow: '0 2px 8px rgba(139, 112, 255, 0.35)',
            fontWeight: 600,
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
