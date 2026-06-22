import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { apiFetch } from "@/lib/api";
import { useLang } from "@/lang/LanguageProvider";

interface DeadlineItem {
  id: number;
  title: string;
  titleSw: string;
  authority: string;
  nextDueDate: string; // ISO date string, e.g. "2026-07-09"
  daysRemaining: number;
}

interface DeadlinesGridProps {
  /** Basic: show 3; Standard: show 6 (UI-level cap, not API). */
  subscriptionTier: "basic" | "standard";
}

function daysLabel(days: number, todayLabel: string, tomorrowLabel: string, daysLeftLabel: string): string {
  if (days === 0) return todayLabel;
  if (days === 1) return tomorrowLabel;
  return `${days} ${daysLeftLabel}`;
}

function urgencyVariant(days: number): "urgent" | "watch" | "outline" {
  if (days <= 3) return "urgent";
  if (days <= 7) return "watch";
  return "outline";
}

export function DeadlinesGrid({ subscriptionTier }: DeadlinesGridProps) {
  const { t, lang } = useLang();

  const { data, isLoading } = useQuery<DeadlineItem[]>({
    queryKey: ["regulatory", "deadlines"],
    queryFn: () => apiFetch<DeadlineItem[]>("/regulatory/deadlines"),
    staleTime: 1000 * 60 * 60, // 1 hour — deadlines don't change intraday
  });

  const visibleCount = subscriptionTier === "standard" ? 6 : 3;
  const items = data?.slice(0, visibleCount) ?? [];
  const totalCount = data?.length ?? 0;

  return (
    <div className="flex flex-col gap-3">
      <h2 className="text-base font-semibold">{t.deadlines.title}</h2>

      {isLoading && (
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          {[1, 2, 3].map((n) => (
            <div key={n} className="h-16 w-full animate-pulse rounded-xl bg-muted" />
          ))}
        </div>
      )}

      {!isLoading && items.length === 0 && (
        <p className="text-sm text-muted-foreground">{t.deadlines.empty}</p>
      )}

      {!isLoading && items.length > 0 && (
        <>
          <div className="divide-y divide-border/30">
            {items.map((item) => {
              const title = lang === "sw" ? item.titleSw : item.title;
              const label = daysLabel(
                item.daysRemaining,
                t.deadlines.today,
                t.deadlines.tomorrow,
                t.deadlines.daysLeft,
              );
              const badgeVariant = urgencyVariant(item.daysRemaining);
              const authorityLabel =
                t.deadlines.authority[item.authority as keyof typeof t.deadlines.authority] ??
                item.authority;

              return (
                <div key={item.id} className="py-3.5 first:pt-0 last:pb-0 flex items-start justify-between gap-3 group">
                  <div className="min-w-0">
                    <p className="text-sm font-bold text-[#201515] leading-snug truncate group-hover:text-primary transition-colors">
                      {title}
                    </p>
                    <p className="text-xs text-muted-foreground mt-0.5 font-medium">
                      {authorityLabel}
                    </p>
                  </div>
                  <Badge variant={badgeVariant} className="shrink-0 text-[10px] font-bold uppercase rounded px-2 py-0.5">
                    {label}
                  </Badge>
                </div>
              );
            })}
          </div>

          {/* Basic: show upgrade nudge when there are more items hidden */}
          {subscriptionTier === "basic" && totalCount > visibleCount && (
            <p className="text-xs text-muted-foreground">
              {t.deadlines.basicLimit} {totalCount}
            </p>
          )}
        </>
      )}
    </div>
  );
}
