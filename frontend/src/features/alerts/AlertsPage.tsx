import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";
import { apiFetch } from "@/lib/api";
import type { AlertsPageData, AlertItem, AlertSeverity } from "./types";

const UNLIMITED_CAP = 2147483647;

const SEVERITY_STYLES: Record<AlertSeverity, { badge: string; border: string }> = {
  URGENT:    { badge: "bg-destructive text-destructive-foreground", border: "border-l-destructive" },
  ACT_NOW:   { badge: "bg-orange-500 text-white", border: "border-l-orange-500" },
  WATCH_OUT: { badge: "bg-yellow-500 text-white", border: "border-l-yellow-500" },
};

export default function AlertsPage() {
  const { t } = useLang();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery<AlertsPageData>({
    queryKey: ["alerts", "me"],
    queryFn: () => apiFetch<AlertsPageData>("/alerts/me"),
  });

  const markRead = useMutation({
    mutationFn: (id: number) =>
      apiFetch<AlertItem>(`/alerts/${id}/read`, { method: "POST" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["alerts", "me"] }),
  });

  const isStandard = user?.subscriptionTier === "standard";
  const isBasic = !isStandard;
  const monthlyCap = data?.monthlyCap === UNLIMITED_CAP ? null : (data?.monthlyCap ?? 3);
  const monthlyUsed = data?.monthlyUsed ?? 0;
  const capPct = monthlyCap ? Math.round((monthlyUsed / monthlyCap) * 100) : 0;

  return (
    <main className="min-h-screen bg-background px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-2xl flex-col gap-6">

        {/* Header */}
        <div className="flex items-center justify-between">
          <Link to="/" className="text-sm text-muted-foreground hover:underline">
            ← {t.dashboard.backToDashboard}
          </Link>
        </div>
        <h1 className="text-2xl font-bold">{t.alerts.title}</h1>

        {/* Basic monthly cap progress */}
        {isBasic && monthlyCap && (
          <Card className="border-muted">
            <CardContent className="pt-5 pb-4">
              <div className="flex items-center justify-between text-sm mb-2">
                <span className="text-muted-foreground">
                  {monthlyUsed} {t.alerts.monthlyCap} {monthlyCap} {t.alerts.monthlyUsed}
                </span>
                <span className="font-medium">{capPct}%</span>
              </div>
              <Progress value={capPct} className="h-2" />
              <p className="mt-2 text-xs text-muted-foreground">{t.alerts.basicNote}</p>
            </CardContent>
          </Card>
        )}

        {/* Alert list */}
        {isLoading ? (
          <div className="flex flex-col gap-3">
            {[1, 2, 3].map((n) => (
              <div key={n} className="h-24 w-full animate-pulse rounded-xl bg-muted" />
            ))}
          </div>
        ) : !data?.alerts.length ? (
          <p className="text-muted-foreground text-sm">{t.alerts.empty}</p>
        ) : (
          <div className="flex flex-col gap-3">
            {data.alerts.map((alert) => (
              <AlertCard
                key={alert.id}
                alert={alert}
                onMarkRead={() => markRead.mutate(alert.id)}
                markReadLabel={t.alerts.markRead}
                severityLabel={
                  t.alerts.severity[alert.severity as AlertSeverity] ?? alert.severity
                }
              />
            ))}
          </div>
        )}

        {/* Upgrade nudge for Basic users after list */}
        {isBasic && (
          <Card className="border-primary/20 bg-primary/5">
            <CardContent className="pt-5 pb-4">
              <p className="text-sm font-semibold mb-1">{t.upgrade.title}</p>
              <p className="text-sm text-muted-foreground mb-3">{t.upgrade.body}</p>
              {/* TODO: wire to real upgrade/payment flow */}
              <Button size="sm" variant="default">{t.upgrade.cta}</Button>
            </CardContent>
          </Card>
        )}
      </div>
    </main>
  );
}

interface AlertCardProps {
  alert: AlertItem;
  onMarkRead: () => void;
  markReadLabel: string;
  severityLabel: string;
}

function AlertCard({ alert, onMarkRead, markReadLabel, severityLabel }: AlertCardProps) {
  const styles = SEVERITY_STYLES[alert.severity as AlertSeverity] ?? SEVERITY_STYLES.WATCH_OUT;
  const dateStr = new Date(alert.createdAt).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  return (
    <Card
      className={`border-l-4 ${styles.border} ${alert.isRead ? "opacity-60" : ""} transition-opacity`}
    >
      <CardContent className="pt-4 pb-3">
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1 flex-wrap">
              <Badge className={`text-xs shrink-0 ${styles.badge}`}>
                {severityLabel}
              </Badge>
              <span className="text-xs text-muted-foreground">{dateStr}</span>
            </div>
            <p className="font-semibold text-sm leading-snug">{alert.title}</p>
            <p className="mt-1 text-sm text-muted-foreground leading-relaxed">{alert.body}</p>
          </div>
          {!alert.isRead && (
            <Button
              size="sm"
              variant="ghost"
              className="shrink-0 text-xs"
              onClick={onMarkRead}
            >
              {markReadLabel}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
