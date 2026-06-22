import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";

export type Tier = "basic" | "standard";

interface EntitlementResponse {
  feature: string;
  requiredTier: Tier;
}

export type EntitlementMap = Record<string, Tier>;

/**
 * Loads the feature -> required-tier matrix from the backend (single source of truth).
 * Cached for the session; refetched rarely since packaging changes are infrequent.
 */
export function useEntitlements() {
  return useQuery({
    queryKey: ["entitlements"],
    queryFn: async (): Promise<EntitlementMap> => {
      const rows = await apiFetch<EntitlementResponse[]>("/entitlements");
      return Object.fromEntries(rows.map((r) => [r.feature, r.requiredTier]));
    },
    staleTime: 1000 * 60 * 60,
  });
}

export const TIER_RANK: Record<Tier, number> = { basic: 0, standard: 1 };
