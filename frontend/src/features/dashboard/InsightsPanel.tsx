import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { apiFetch } from "@/lib/api";
import { useLang } from "@/lang/LanguageProvider";

interface InsightResponse {
  id: number;
  title: string;
  body: string;
  actionText: string | null;
  moduleCode: string | null;
  isRead: boolean;
  generatedAt: string;
}

export type { InsightResponse };

const MODULE_BADGE_VARIANT: Record<string, "urgent" | "act" | "watch" | "good" | "outline"> = {
  FX:           "watch",
  LIQUIDITY:    "act",
  COMMODITY:    "act",
  CREDIT:       "watch",
  REGULATORY:   "urgent",
  COUNTERPARTY: "watch",
  MACRO:        "watch",
};

/** Full insight list — uncapped, used on the /insights page. */
export function InsightsPanel() {
  const { t } = useLang();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["me-insights"],
    queryFn: () => apiFetch<InsightResponse[]>("/me/insights"),
    staleTime: 1000 * 60 * 5,
  });

  const markRead = useMutation({
    mutationFn: (id: number) =>
      apiFetch<InsightResponse>(`/me/insights/${id}/read`, { method: "POST", body: JSON.stringify({}) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["me-insights"] }),
  });

  const insights = data ?? [];

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-28 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (insights.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Sparkles className="h-8 w-8 text-muted-foreground/30" />
        <p className="text-sm text-muted-foreground max-w-xs">{t.insights.empty}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {insights.map((insight) => (
        <div
          key={insight.id}
          onClick={() => !insight.isRead && markRead.mutate(insight.id)}
          className={`group rounded-lg border px-4 py-3 flex flex-col gap-2 transition-colors cursor-pointer
            ${insight.isRead
              ? "border-border/20 bg-muted/5"
              : "border-primary/20 bg-primary/5 hover:bg-primary/8"
            }`}
        >
          <div className="flex items-start gap-1.5 flex-wrap">
            {insight.moduleCode && (
              <Badge
                variant={MODULE_BADGE_VARIANT[insight.moduleCode] ?? "outline"}
                className="text-[10px] px-1.5 py-0.5"
              >
                {insight.moduleCode}
              </Badge>
            )}
            {!insight.isRead && (
              <span className="h-1.5 w-1.5 rounded-full bg-primary shrink-0 mt-1" />
            )}
          </div>
          <p className="text-sm font-semibold text-[#201515] leading-snug">{insight.title}</p>
          <p className="text-xs text-muted-foreground leading-relaxed">{insight.body}</p>
          {insight.actionText && (
            <div className="flex items-start gap-1.5 mt-0.5">
              <span className="text-[10px] font-bold uppercase tracking-wider text-primary shrink-0 mt-0.5">
                {t.insights.actionPrefix}
              </span>
              <span className="text-xs text-primary font-medium leading-relaxed">
                {insight.actionText}
              </span>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
