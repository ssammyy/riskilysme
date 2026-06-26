import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { apiFetch } from "@/lib/api";
import { useLang } from "@/lang/LanguageProvider";
import { UpgradePrompt } from "@/components/UpgradePrompt";
import { useAuth } from "@/auth/AuthProvider";
import { CheckCheck, Clock, Bell } from "lucide-react";
import { cn } from "@/lib/utils";

type DeadlineStatusValue = "DONE" | "IN_PROGRESS" | "REMIND_ME";

interface DeadlineItem {
  id: number;
  title: string;
  titleSw: string;
  authority: string;
  nextDueDate: string;
  daysRemaining: number;
  userStatus: DeadlineStatusValue | null;
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

const STATUS_OPTIONS: { value: DeadlineStatusValue; icon: typeof CheckCheck }[] = [
  { value: "DONE",        icon: CheckCheck },
  { value: "IN_PROGRESS", icon: Clock },
  { value: "REMIND_ME",   icon: Bell },
];

function statusLabel(value: DeadlineStatusValue, t: ReturnType<typeof useLang>["t"]): string {
  switch (value) {
    case "DONE":        return t.deadlines.status.done;
    case "IN_PROGRESS": return t.deadlines.status.inProgress;
    case "REMIND_ME":   return t.deadlines.status.remindMe;
  }
}

export function DeadlinesGrid({ subscriptionTier }: DeadlinesGridProps) {
  const { t, lang } = useLang();
  const { status: authStatus } = useAuth();
  const queryClient = useQueryClient();
  const isAuthenticated = authStatus === "authenticated";

  const { data, isLoading } = useQuery<DeadlineItem[]>({
    queryKey: ["regulatory", "deadlines"],
    queryFn: () => apiFetch<DeadlineItem[]>("/regulatory/deadlines"),
    staleTime: 1000 * 60 * 60,
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: DeadlineStatusValue }) =>
      apiFetch<void>(`/regulatory/deadlines/${id}/status`, {
        method: "PUT",
        body: JSON.stringify({ status }),
      }),
    onMutate: async ({ id, status }) => {
      await queryClient.cancelQueries({ queryKey: ["regulatory", "deadlines"] });
      const prev = queryClient.getQueryData<DeadlineItem[]>(["regulatory", "deadlines"]);
      queryClient.setQueryData<DeadlineItem[]>(["regulatory", "deadlines"], (old) =>
        old?.map((item) => (item.id === id ? { ...item, userStatus: status } : item)),
      );
      return { prev };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.prev) queryClient.setQueryData(["regulatory", "deadlines"], ctx.prev);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["regulatory", "deadlines"] });
    },
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
            <div key={n} className="h-20 w-full animate-pulse rounded-xl bg-muted" />
          ))}
        </div>
      )}

      {!isLoading && items.length === 0 && (
        <p className="text-sm text-muted-foreground">{t.deadlines.empty}</p>
      )}

      {!isLoading && items.length > 0 && (
        <>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-1">
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
              const effectiveStatus: DeadlineStatusValue = item.userStatus ?? "REMIND_ME";
              const isPending = statusMutation.isPending && statusMutation.variables?.id === item.id;

              return (
                <div
                  key={item.id}
                  className={cn(
                    "flex flex-col gap-2.5 rounded-lg border border-border/30 bg-muted/10 px-3 py-2.5 transition-opacity",
                    effectiveStatus === "DONE" && "opacity-60",
                  )}
                >
                  {/* Title row */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className={cn(
                        "text-sm font-bold text-[#201515] leading-snug truncate transition-colors",
                        effectiveStatus === "DONE" && "line-through text-muted-foreground",
                      )}>
                        {title}
                      </p>
                      <p className="text-xs text-muted-foreground mt-0.5 font-medium">
                        {authorityLabel}
                      </p>
                    </div>
                    <Badge
                      variant={badgeVariant}
                      className="shrink-0 text-[10px] font-bold uppercase rounded px-2 py-0.5"
                    >
                      {label}
                    </Badge>
                  </div>

                  {/* Status toggle — only for authenticated users */}
                  {isAuthenticated && (
                    <div className="flex gap-1">
                      {STATUS_OPTIONS.map(({ value, icon: Icon }) => {
                        const active = effectiveStatus === value;
                        return (
                          <Button
                            key={value}
                            size="sm"
                            variant={active ? "secondary" : "ghost"}
                            className={cn(
                              "h-6 gap-1 rounded-full px-2 py-0 text-[10px] font-semibold",
                              active && "bg-[#201515] text-[#fffefb] hover:bg-[#2f2a26]",
                              !active && "text-muted-foreground hover:text-[#201515]",
                            )}
                            disabled={isPending}
                            onClick={() =>
                              statusMutation.mutate({ id: item.id, status: value })
                            }
                          >
                            <Icon className="h-2.5 w-2.5" />
                            {statusLabel(value, t)}
                          </Button>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {subscriptionTier === "basic" && totalCount > visibleCount && (
            <UpgradePrompt variant="inline" />
          )}
        </>
      )}
    </div>
  );
}
