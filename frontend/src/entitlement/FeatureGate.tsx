import type { ReactNode } from "react";
import { useFeature } from "./useFeature";

interface FeatureGateProps {
  feature: string;
  children: ReactNode;
  /** Shown when the user's tier doesn't include the feature (e.g. an upgrade prompt). */
  fallback?: ReactNode;
}

/**
 * Renders children only if the current tier includes the feature, otherwise the fallback.
 * The full upgrade-prompt UI is built in Sprint 4; this is the gating primitive it will use.
 */
export function FeatureGate({ feature, children, fallback = null }: FeatureGateProps) {
  const { allowed, loading } = useFeature(feature);
  if (loading) return null;
  return <>{allowed ? children : fallback}</>;
}
