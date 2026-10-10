import React, { useId } from 'react';
import { useAnimationVisibility } from '../theme/motion';

export type ContextFieldMode =
  | 'IDLE'
  | 'PROTECTION_ACTIVE'
  | 'ANALYZING'
  | 'ACT'
  | 'ASK'
  | 'WARN'
  | 'STOP';

interface ContextFieldProps {
  mode?: ContextFieldMode;
  size?: number | string;
  className?: string;
  focusedEvidenceAngle?: number | null;
}

/**
 * THE CONTEXT FIELD (Spectral Signal)
 *
 * An original animated visual motif representing a signal passing through a field of uncertainty.
 * Concentric offset contours surround a central aperture with signal points moving along contours.
 * State transitions:
 *  - IDLE: quiet breathing field
 *  - PROTECTION_ACTIVE: subtle synchronized orbit indicating live monitoring
 *  - ANALYZING: directional wavefront signal sweeping through contours
 *  - ASK: soft amber field with a visible unresolved angular gap
 *  - WARN: focused orange signal sector highlighting specific evidence
 *  - STOP: geometric contraction around a dense coral focal core
 *  - ACT: contours align and settle into calm Signal Lime
 */
export const ContextField: React.FC<ContextFieldProps> = ({
  mode = 'IDLE',
  size = 120,
  className = '',
  focusedEvidenceAngle = null,
}) => {
  const uniqueId = useId().replace(/:/g, '_');
  const isVisible = useAnimationVisibility();

  // Palette resolution
  const colors = {
    violet: '#8B70FF',
    cyan: '#45E4FF',
    lime: '#D8FF63',
    actLime: '#C9F77A',
    askAmber: '#FFD166',
    warnOrange: '#FFAA65',
    stopCoral: '#FF667D',
    muted: '#9CA8C2',
  };

  const getPrimaryColor = () => {
    switch (mode) {
      case 'ACT':
        return colors.actLime;
      case 'ASK':
        return colors.askAmber;
      case 'WARN':
        return colors.warnOrange;
      case 'STOP':
        return colors.stopCoral;
      case 'ANALYZING':
        return colors.cyan;
      case 'PROTECTION_ACTIVE':
        return colors.violet;
      case 'IDLE':
      default:
        return colors.violet;
    }
  };

  const primaryColor = getPrimaryColor();

  // Contraction scale for STOP
  const contractionScale = mode === 'STOP' ? 0.82 : 1.0;

  return (
    <div
      className={`context-field-root context-field-root-${uniqueId} ${className}`}
      style={{
        width: size,
        height: size,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        userSelect: 'none',
      }}
    >
      <svg
        viewBox="0 0 200 200"
        width="100%"
        height="100%"
        style={{ overflow: 'visible' }}
      >
        <defs>
          {/* Radial Aperture Glow */}
          <radialGradient id={`aperture-glow-${uniqueId}`} cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor={primaryColor} stopOpacity="0.45" />
            <stop offset="60%" stopColor={primaryColor} stopOpacity="0.12" />
            <stop offset="100%" stopColor={primaryColor} stopOpacity="0" />
          </radialGradient>

          {/* Sweep Beam Gradient for ANALYZING */}
          <linearGradient id={`sweep-grad-${uniqueId}`} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor={colors.cyan} stopOpacity="0.8" />
            <stop offset="50%" stopColor={colors.violet} stopOpacity="0.3" />
            <stop offset="100%" stopColor={colors.cyan} stopOpacity="0" />
          </linearGradient>

          {/* Keyframe animations */}
          <style>{`
            @keyframes cf-pulse-${uniqueId} {
              0%, 100% { transform: scale(1); opacity: 0.9; }
              50% { transform: scale(1.08); opacity: 0.55; }
            }
            @keyframes cf-rotate-cw-${uniqueId} {
              from { transform: rotate(0deg); }
              to { transform: rotate(360deg); }
            }
            @keyframes cf-rotate-ccw-${uniqueId} {
              from { transform: rotate(360deg); }
              to { transform: rotate(0deg); }
            }
            @keyframes cf-sweep-${uniqueId} {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
            @keyframes cf-breathe-${uniqueId} {
              0%, 100% { transform: scale(1); }
              50% { transform: scale(1.03); }
            }
            .cf-center-${uniqueId} {
              transform-origin: 100px 100px;
            }
            .cf-animated-pulse-${uniqueId} {
              animation: cf-pulse-${uniqueId} 3.2s ease-in-out infinite;
            }
            .cf-animated-orbit-cw-${uniqueId} {
              animation: cf-rotate-cw-${uniqueId} 18s linear infinite;
            }
            .cf-animated-orbit-ccw-${uniqueId} {
              animation: cf-rotate-ccw-${uniqueId} 26s linear infinite;
            }
            .cf-animated-sweep-${uniqueId} {
              animation: cf-sweep-${uniqueId} 2.4s cubic-bezier(0.4, 0, 0.2, 1) infinite;
            }
            .cf-breathe-${uniqueId} {
              animation: cf-breathe-${uniqueId} 5s ease-in-out infinite;
            }
            ${!isVisible ? `
            .context-field-root-${uniqueId} * {
              animation-play-state: paused !important;
            }
            ` : ''}
          `}</style>
        </defs>

        {/* Ambient background glow */}
        <circle
          cx="100"
          cy="100"
          r="84"
          fill={`url(#aperture-glow-${uniqueId})`}
          className={`cf-center-${uniqueId} cf-animated-pulse-${uniqueId}`}
        />

        {/* Scaled group for geometry transformations */}
        <g
          transform={`scale(${contractionScale})`}
          style={{ transformOrigin: '100px 100px', transition: 'transform 0.6s cubic-bezier(0.2, 0.8, 0.2, 1)' }}
        >
          {/* Contour 1 - Inner Aperture Rim */}
          <circle
            cx="100"
            cy="100"
            r="28"
            fill="none"
            stroke={primaryColor}
            strokeWidth="1.2"
            strokeOpacity={mode === 'STOP' ? '0.85' : '0.4'}
            strokeDasharray={mode === 'ASK' ? '140 35' : 'none'}
          />

          {/* Contour 2 - Offset Ellipse / Contour */}
          <g className={`cf-center-${uniqueId} ${mode === 'ANALYZING' || mode === 'PROTECTION_ACTIVE' ? `cf-animated-orbit-cw-${uniqueId}` : `cf-breathe-${uniqueId}`}`}>
            <ellipse
              cx="100"
              cy="100"
              rx="46"
              ry="43"
              fill="none"
              stroke={primaryColor}
              strokeWidth="1"
              strokeOpacity="0.45"
              strokeDasharray={mode === 'ASK' ? '220 50' : mode === 'WARN' ? '80 180' : 'none'}
            />
            {/* Signal point on Contour 2 */}
            <circle
              cx="146"
              cy="100"
              r={mode === 'WARN' ? 3.5 : 2.5}
              fill={mode === 'WARN' ? colors.warnOrange : primaryColor}
              filter={mode === 'WARN' ? 'drop-shadow(0 0 4px #FFAA65)' : undefined}
            />
          </g>

          {/* Contour 3 - Mid Field */}
          <g className={`cf-center-${uniqueId} ${mode === 'ANALYZING' || mode === 'PROTECTION_ACTIVE' ? `cf-animated-orbit-ccw-${uniqueId}` : ''}`}>
            <ellipse
              cx="100"
              cy="100"
              rx="64"
              ry="66"
              fill="none"
              stroke={mode === 'ACT' ? colors.actLime : colors.cyan}
              strokeWidth="1"
              strokeOpacity={mode === 'IDLE' ? '0.25' : '0.5'}
              strokeDasharray={mode === 'ASK' ? '310 65' : 'none'}
            />
            {/* Signal point on Contour 3 */}
            <circle
              cx="100"
              cy="34"
              r="2"
              fill={colors.cyan}
              opacity={mode === 'IDLE' ? 0.4 : 0.9}
            />
            <circle
              cx="100"
              cy="166"
              r="2"
              fill={colors.cyan}
              opacity={mode === 'IDLE' ? 0.3 : 0.8}
            />
          </g>

          {/* Contour 4 - Outer Signal Perimeter */}
          <g className={`cf-center-${uniqueId} ${mode === 'ANALYZING' ? `cf-animated-orbit-cw-${uniqueId}` : ''}`}>
            <ellipse
              cx="100"
              cy="100"
              rx="82"
              ry="80"
              fill="none"
              stroke={primaryColor}
              strokeWidth="0.85"
              strokeOpacity={mode === 'IDLE' ? '0.2' : '0.4'}
              strokeDasharray={mode === 'ASK' ? '390 85' : 'none'}
            />
            {/* Signal point on Contour 4 */}
            {(mode === 'PROTECTION_ACTIVE' || mode === 'ANALYZING' || mode === 'ACT') && (
              <circle
                cx="182"
                cy="100"
                r="2.5"
                fill={primaryColor}
                filter="drop-shadow(0 0 3px currentColor)"
              />
            )}
          </g>

          {/* Directional Wavefront Beam for ANALYZING mode */}
          {mode === 'ANALYZING' && (
            <g className={`cf-center-${uniqueId} cf-animated-sweep-${uniqueId}`}>
              <path
                d="M 100 100 L 175 60 A 84 84 0 0 1 184 100 Z"
                fill={`url(#sweep-grad-${uniqueId})`}
              />
              <line
                x1="100"
                y1="100"
                x2="184"
                y2="100"
                stroke={colors.cyan}
                strokeWidth="1.5"
                strokeLinecap="round"
                filter="drop-shadow(0 0 4px #45E4FF)"
              />
            </g>
          )}

          {/* WARN Mode: Focused Evidence Sector Highlight */}
          {mode === 'WARN' && (
            <path
              d="M 100 100 L 158 50 A 75 75 0 0 1 175 100 Z"
              fill={colors.warnOrange}
              fillOpacity="0.18"
              stroke={colors.warnOrange}
              strokeWidth="1"
              strokeDasharray="4 2"
            />
          )}

          {/* ASK Mode: Visible Unresolved Angular Gap Annotation */}
          {mode === 'ASK' && (
            <g>
              <line
                x1="100"
                y1="100"
                x2="160"
                y2="40"
                stroke={colors.askAmber}
                strokeWidth="0.8"
                strokeDasharray="2 3"
                opacity="0.6"
              />
              <line
                x1="100"
                y1="100"
                x2="170"
                y2="80"
                stroke={colors.askAmber}
                strokeWidth="0.8"
                strokeDasharray="2 3"
                opacity="0.6"
              />
            </g>
          )}

          {/* 3. Evidence Focus: Oriented contour connecting evidence source to explanation */}
          {focusedEvidenceAngle != null && (
            <g style={{ transition: 'all 0.35s cubic-bezier(0.2, 0, 0, 1)' }}>
              <line
                x1="100"
                y1="100"
                x2={100 + 78 * Math.cos((focusedEvidenceAngle * Math.PI) / 180)}
                y2={100 + 78 * Math.sin((focusedEvidenceAngle * Math.PI) / 180)}
                stroke={colors.cyan}
                strokeWidth="1.6"
                strokeDasharray="4 3"
                filter="drop-shadow(0 0 6px #45E4FF)"
                opacity="0.9"
              />
              <circle
                cx={100 + 78 * Math.cos((focusedEvidenceAngle * Math.PI) / 180)}
                cy={100 + 78 * Math.sin((focusedEvidenceAngle * Math.PI) / 180)}
                r="4.5"
                fill={colors.cyan}
                filter="drop-shadow(0 0 8px #45E4FF)"
              />
              <circle
                cx={100 + 78 * Math.cos((focusedEvidenceAngle * Math.PI) / 180)}
                cy={100 + 78 * Math.sin((focusedEvidenceAngle * Math.PI) / 180)}
                r="8"
                fill="none"
                stroke={colors.cyan}
                strokeWidth="1"
                strokeOpacity="0.5"
              />
            </g>
          )}

          {/* Central Aperture Focal Point */}
          <circle
            cx="100"
            cy="100"
            r={mode === 'STOP' ? 14 : mode === 'ANALYZING' ? 9 : 8}
            fill={primaryColor}
            filter={`drop-shadow(0 0 ${mode === 'STOP' ? 8 : 4}px ${primaryColor})`}
            className={`cf-center-${uniqueId} ${mode === 'IDLE' || mode === 'PROTECTION_ACTIVE' ? `cf-animated-pulse-${uniqueId}` : ''}`}
          />
          <circle
            cx="100"
            cy="100"
            r="3"
            fill="#FFFFFF"
            opacity="0.85"
          />
        </g>
      </svg>
    </div>
  );
};
