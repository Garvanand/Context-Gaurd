import React from 'react';

interface ApertureSignalLogoProps {
  size?: number;
  variant?: 'symbol' | 'horizontal' | 'appbar';
  animated?: boolean;
  className?: string;
  style?: React.CSSProperties;
}

export const ApertureSignalLogo: React.FC<ApertureSignalLogoProps> = ({
  size = 36,
  variant = 'symbol',
  animated = false,
  className = '',
  style = {},
}) => {
  if (variant === 'horizontal') {
    const height = size;
    const width = Math.round(size * (340 / 68)); // 5:1 ratio
    return (
      <svg
        viewBox="0 0 340 68"
        width={width}
        height={height}
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className={className}
        style={{ display: 'block', overflow: 'visible', ...style }}
        aria-label="ContextGuard Aperture Signal Logo"
      >
        <defs>
          <linearGradient id="nav_h_wing_alpha" x1="12" y1="12" x2="88" y2="88" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#A28DFF" />
            <stop offset="60%" stopColor="#8B70FF" />
            <stop offset="100%" stopColor="#6E4FFF" />
          </linearGradient>
          <linearGradient id="nav_h_wing_beta" x1="30" y1="30" x2="70" y2="70" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#5BF0FF" />
            <stop offset="100%" stopColor="#2FD5F6" />
          </linearGradient>
          <linearGradient id="nav_h_guard_grad" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stopColor="#8B70FF" />
            <stop offset="100%" stopColor="#5BF0FF" />
          </linearGradient>
        </defs>

        <g transform="translate(10, 10) scale(0.48)">
          <path
            d="M 87.42 43.40 A 38 38 0 1 1 43.40 12.58 L 45.14 22.43 A 28 28 0 1 0 77.57 45.14 Z"
            fill="url(#nav_h_wing_alpha)"
          />
          <path
            d="M 64.78 67.62 A 23 23 0 1 1 57.87 28.39 L 55.13 35.90 A 15 15 0 1 0 59.64 61.49 Z"
            fill="url(#nav_h_wing_beta)"
          />
          <path
            d="M 42.72 55.44 A 9 9 0 0 1 55.44 42.72 Z"
            fill="#D8FF63"
          />
          <circle cx="55.0" cy="55.0" r="2.8" fill="#F4F6FF" />
        </g>

        <g transform="translate(70, 0)">
          <text
            x="0"
            y="35"
            fontFamily="'Space Grotesk', -apple-system, sans-serif"
            fontSize="22"
            fontWeight="700"
            letterSpacing="1.5"
            fill="#F4F6FF"
          >
            CONTEXT<tspan fill="url(#nav_h_guard_grad)">GUARD</tspan>
          </text>
          <text
            x="1"
            y="50"
            fontFamily="'JetBrains Mono', monospace"
            fontSize="8.5"
            fontWeight="600"
            letterSpacing="2.8"
            fill="#8B95B5"
          >
            PRE-ACTION SAFETY RETICLE
          </text>
        </g>
      </svg>
    );
  }

  // Standalone Symbol (1:1 aspect ratio)
  return (
    <svg
      viewBox="0 0 100 100"
      width={size}
      height={size}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      style={{
        display: 'inline-block',
        verticalAlign: 'middle',
        overflow: 'visible',
        filter: animated ? 'drop-shadow(0 0 12px rgba(139, 112, 255, 0.45))' : undefined,
        transition: 'transform 0.3s ease, filter 0.3s ease',
        ...style,
      }}
      aria-label="ContextGuard Aperture Signal Mark"
    >
      <defs>
        <linearGradient id={`cg_sym_a_${size}`} x1="12" y1="12" x2="88" y2="88" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#A28DFF" />
          <stop offset="60%" stopColor="#8B70FF" />
          <stop offset="100%" stopColor="#6E4FFF" />
        </linearGradient>
        <linearGradient id={`cg_sym_b_${size}`} x1="30" y1="30" x2="70" y2="70" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#5BF0FF" />
          <stop offset="100%" stopColor="#2FD5F6" />
        </linearGradient>
      </defs>

      {/* Wing Alpha: Outer Signal Contour (270° incomplete aperture with engineered 90° intake gap) */}
      <path
        d="M 87.42 43.40 A 38 38 0 1 1 43.40 12.58 L 45.14 22.43 A 28 28 0 1 0 77.57 45.14 Z"
        fill={`url(#cg_sym_a_${size})`}
      />

      {/* Wing Beta: Inner Offset Signal Contour (240° concentric offset channel) */}
      <path
        d="M 64.78 67.62 A 23 23 0 1 1 57.87 28.39 L 55.13 35.90 A 15 15 0 1 0 59.64 61.49 Z"
        fill={`url(#cg_sym_b_${size})`}
      />

      {/* Central Aperture Focal Core (Action-Gating Hemisphere with 45° Discontinuity Slit) */}
      <path
        d="M 42.72 55.44 A 9 9 0 0 1 55.44 42.72 Z"
        fill="#D8FF63"
      />

      {/* Observer Anchor Point */}
      <circle cx="55.0" cy="55.0" r="2.8" fill="#F4F6FF" />
    </svg>
  );
};
