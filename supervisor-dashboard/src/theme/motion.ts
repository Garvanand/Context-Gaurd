/**
 * ContextGuard Motion Token System (Web / Supervisor Dashboard)
 *
 * Core Philosophy:
 * Motion communicates Attention, State Change, Evidence, Uncertainty,
 * User Choice, and System Readiness.
 */

export const MotionTokens = {
  // Durations (ms)
  durationFast: 150,
  durationStandard: 260,
  durationMajor: 420,
  durationSettle: 500,

  // CSS Easing
  easeEmphasized: 'cubic-bezier(0.2, 0.0, 0.0, 1.0)',
  easeStandard: 'cubic-bezier(0.4, 0.0, 0.2, 1.0)',
  easeDecelerate: 'cubic-bezier(0.0, 0.0, 0.2, 1.0)',
} as const;

/**
 * Checks if the user or browser prefers reduced motion.
 */
export function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined') return false;
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * React hook for observing document visibility to pause off-screen animations.
 */
import { useEffect, useState } from 'react';

export function useAnimationVisibility(): boolean {
  const [isVisible, setIsVisible] = useState(() =>
    typeof document !== 'undefined' ? document.visibilityState === 'visible' : true
  );

  useEffect(() => {
    const handleVisibilityChange = () => {
      setIsVisible(document.visibilityState === 'visible');
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  return isVisible;
}
