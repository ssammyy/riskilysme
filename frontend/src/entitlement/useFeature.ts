import { useAuth } from "@/auth/AuthProvider";
import { TIER_RANK, useEntitlements, type Tier } from "./useEntitlements";

export interface FeatureAccess {
  allowed: boolean;
  loading: boolean;
  requiredTier: Tier | null;
}

/**
 * Whether the current user's tier includes a feature, resolved against the backend matrix.
 * While the matrix or session is loading, `allowed` is false and `loading` is true.
 */
export function useFeature(feature: string): FeatureAccess {
  const { user } = useAuth();
  const { data, isLoading } = useEntitlements();

  if (isLoading || !data || !user) {
    return { allowed: false, loading: isLoading, requiredTier: null };
  }
  const requiredTier = data[feature] ?? "basic";
  const allowed = TIER_RANK[user.subscriptionTier] >= TIER_RANK[requiredTier];
  return { allowed, loading: false, requiredTier };
}
