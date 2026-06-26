import { useQuery } from "@tanstack/react-query";
import { Link, Navigate } from "react-router-dom";
import { ArrowRight, Calendar, Sparkles } from "lucide-react";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";
import { apiFetch } from "@/lib/api";
import type { ScoreResponse } from "@/score/types";
import { ActionBanner } from "./ActionBanner";
import type { InsightResponse } from "./InsightsPanel";
import { MarketDataStrip } from "./MarketDataStrip";
import { ScoreGauge } from "./ScoreGauge";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { BandCode, ModuleCode, ModuleScore } from "@/score/types";

// ─── Compact module tile ──────────────────────────────────────────────────────

const BAND_VARIANT: Record<BandCode, "good" | "watch" | "act" | "urgent"> = {
  all_good:  "good",
  watch_out: "watch",
  act_now:   "act",
  urgent:    "urgent",
};

function ModuleTile({ module }: { module: ModuleScore }) {
  const { t } = useLang();
  const code = module.code as ModuleCode;
  const name = t.modules.names[code] ?? code;

  return (
    <Link
      to={`/modules/${module.code.toLowerCase()}`}
      className="block rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <Card className="h-full border border-border/40 bg-card/30 shadow-sm hover:bg-card hover:shadow-md transition-all">
        <CardContent className="p-4 flex flex-col gap-3">
          <p className="text-sm font-semibold text-[#201515] leading-snug">{name}</p>
          <div className="flex items-center justify-between gap-2">
            <Badge
              variant={BAND_VARIANT[module.band]}
              className="text-[9px] px-1.5 py-0 rounded-full font-bold uppercase tracking-wider"
            >
              {t.score.band[module.band]}
            </Badge>
            <span className="font-display text-3xl font-extrabold text-[#201515] leading-none">
              {module.health}
            </span>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

// ─── Compact deadline preview card ───────────────────────────────────────────

interface DeadlineItem {
  id: number;
  title: string;
  titleSw: string;
  authority: string;
  daysRemaining: number;
  userStatus: string | null;
}

function DeadlinesCta({ lang }: { lang: string }) {
  const { t } = useLang();

  const { data, isLoading } = useQuery<DeadlineItem[]>({
    queryKey: ["regulatory", "deadlines"],
    queryFn: () => apiFetch<DeadlineItem[]>("/regulatory/deadlines"),
    staleTime: 1000 * 60 * 60,
  });

  const nearest = data?.slice(0, 2) ?? [];

  return (
    <Card className="border border-border/40 shadow-sm bg-card/25">
      <CardContent className="p-5 flex flex-col gap-4">
        {/* Header row */}
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-primary shrink-0" />
            <h2 className="text-sm font-bold text-[#201515]">{t.deadlines.title}</h2>
          </div>
          <Button variant="ghost" size="sm" asChild className="h-7 px-2 text-xs font-semibold text-primary hover:text-primary/80 -mr-1">
            <Link to="/deadlines">
              {t.deadlines.showMore}
              <ArrowRight className="h-3 w-3 ml-1" />
            </Link>
          </Button>
        </div>

        {/* Skeleton */}
        {isLoading && (
          <div className="space-y-2">
            {[1, 2].map((n) => (
              <div key={n} className="h-10 w-full animate-pulse rounded-lg bg-muted" />
            ))}
          </div>
        )}

        {/* Empty */}
        {!isLoading && nearest.length === 0 && (
          <p className="text-xs text-muted-foreground">{t.deadlines.empty}</p>
        )}

        {/* Nearest 2 deadlines */}
        {!isLoading && nearest.length > 0 && (
          <div className="space-y-2">
            {nearest.map((item) => {
              const title = lang === "sw" ? item.titleSw : item.title;
              const daysLabel =
                item.daysRemaining === 0 ? t.deadlines.today
                : item.daysRemaining === 1 ? t.deadlines.tomorrow
                : `${item.daysRemaining} ${t.deadlines.daysLeft}`;
              const isDone = item.userStatus === "DONE";
              const urgencyColor =
                item.daysRemaining <= 3 ? "text-red-600"
                : item.daysRemaining <= 7 ? "text-amber-600"
                : "text-primary";

              return (
                <div
                  key={item.id}
                  className="flex items-center justify-between gap-3 rounded-lg bg-muted/20 border border-border/20 px-3 py-2"
                >
                  <p className={`text-xs font-semibold truncate ${isDone ? "line-through text-muted-foreground" : "text-[#201515]"}`}>
                    {title}
                  </p>
                  <span className={`text-[10px] font-bold shrink-0 ${isDone ? "text-muted-foreground" : urgencyColor}`}>
                    {isDone ? t.deadlines.status.done : daysLabel}
                  </span>
                </div>
              );
            })}
          </div>
        )}

        {/* Total count hint */}
        {!isLoading && (data?.length ?? 0) > 2 && (
          <p className="text-[10px] text-muted-foreground">
            +{(data?.length ?? 0) - 2} more ·{" "}
            <Link to="/deadlines" className="text-primary hover:underline font-medium">
              {t.deadlines.showMore}
            </Link>
          </p>
        )}
      </CardContent>
    </Card>
  );
}

// ─── Compact insights preview card ───────────────────────────────────────────

const MODULE_BADGE_VARIANT: Record<string, "urgent" | "act" | "watch" | "good" | "outline"> = {
  FX: "watch", LIQUIDITY: "act", COMMODITY: "act", CREDIT: "watch",
  REGULATORY: "urgent", COUNTERPARTY: "watch", MACRO: "watch",
};

function InsightsCta() {
  const { t } = useLang();

  const { data, isLoading } = useQuery({
    queryKey: ["me-insights"],
    queryFn: () => apiFetch<InsightResponse[]>("/me/insights"),
    staleTime: 1000 * 60 * 5,
  });

  const preview = (data ?? []).slice(0, 2);

  return (
    <Card className="border border-border/40 shadow-sm bg-card/25">
      <CardContent className="p-5 flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-primary shrink-0" />
            <h2 className="text-sm font-bold text-[#201515]">{t.insights.title}</h2>
          </div>
          <Button
            variant="ghost"
            size="sm"
            asChild
            className="h-7 px-2 text-xs font-semibold text-primary hover:text-primary/80 -mr-1"
          >
            <Link to="/insights">
              {t.insights.showMore}
              <ArrowRight className="h-3 w-3 ml-1" />
            </Link>
          </Button>
        </div>

        {/* Skeleton */}
        {isLoading && (
          <div className="space-y-2">
            {[1, 2].map((n) => (
              <div key={n} className="h-12 w-full animate-pulse rounded-lg bg-muted" />
            ))}
          </div>
        )}

        {/* Empty */}
        {!isLoading && preview.length === 0 && (
          <p className="text-xs text-muted-foreground">{t.insights.empty}</p>
        )}

        {/* Preview cards */}
        {!isLoading && preview.length > 0 && (
          <div className="space-y-2">
            {preview.map((insight) => (
              <Link
                key={insight.id}
                to="/insights"
                className="flex items-start gap-2 rounded-lg bg-muted/20 border border-border/20 px-3 py-2 hover:bg-muted/40 transition-colors"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-1.5 mb-1">
                    {insight.moduleCode && (
                      <Badge
                        variant={MODULE_BADGE_VARIANT[insight.moduleCode] ?? "outline"}
                        className="text-[9px] px-1 py-0"
                      >
                        {insight.moduleCode}
                      </Badge>
                    )}
                    {!insight.isRead && (
                      <span className="h-1.5 w-1.5 rounded-full bg-primary shrink-0" />
                    )}
                  </div>
                  <p className="text-xs font-semibold text-[#201515] leading-snug line-clamp-2">
                    {insight.title}
                  </p>
                </div>
                <ArrowRight className="h-3 w-3 text-muted-foreground/40 shrink-0 mt-1" />
              </Link>
            ))}
          </div>
        )}

        {/* More hint */}
        {!isLoading && (data?.length ?? 0) > 2 && (
          <p className="text-[10px] text-muted-foreground">
            +{(data?.length ?? 0) - 2} more ·{" "}
            <Link to="/insights" className="text-primary hover:underline font-medium">
              {t.insights.showMore}
            </Link>
          </p>
        )}
      </CardContent>
    </Card>
  );
}

// ─── Dashboard page ───────────────────────────────────────────────────────────

export default function DashboardPage() {
  const { t, lang } = useLang();
  const { user } = useAuth();

  if (user?.role === "admin") return <Navigate to="/admin" replace />;

  const { data: score, isError, error } = useQuery({
    queryKey: ["score", "me"],
    queryFn: () => apiFetch<ScoreResponse>("/score/me"),
    retry: false,
  });

  const apiError = error as { status?: number } | null;
  if (isError && apiError?.status === 404) {
    return <Navigate to="/onboarding" replace />;
  }

  const tier = user?.subscriptionTier ?? "basic";

  return (
    <main className="min-h-screen bg-background px-6 py-8 sm:px-8">
      <div className="mx-auto max-w-7xl flex flex-col gap-6">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-border/20 pb-4">
          <div>
            <h1 className="font-display text-2xl font-bold tracking-tight text-[#201515]">
              {t.nav.dashboard}
            </h1>
            <p className="text-xs text-muted-foreground mt-0.5">
              {lang === "en"
                ? "Real-time risk analytics and compliance tracking for your business."
                : "Uchambuzi wa hatari wa wakati halisi na ufuatiliaji wa utiifu wa biashara yako."}
            </p>
          </div>
        </div>

        {/* 12-column grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Left column — primary stats */}
          <div className="lg:col-span-8 space-y-6">
            <ActionBanner
              firstName={user?.firstName ?? null}
              modules={score?.modules ?? []}
            />

            {score ? (
              <ScoreGauge
                score={score.overallHealth}
                band={score.overallBand}
                calculatedAt={score.calculatedAt}
                subscriptionTier={tier}
              />
            ) : (
              !isError && (
                <div className="flex h-32 items-center justify-center rounded-xl border border-dashed border-border/80 bg-card/30">
                  <p className="text-sm text-muted-foreground">{t.dashboard.noScore}</p>
                </div>
              )
            )}

            <MarketDataStrip />

            {/* Risk module tiles */}
            {score && score.modules.length > 0 && (
              <div className="space-y-3">
                <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground border-b border-border/30 pb-2">
                  {t.dashboard.modulesTitle}
                </h2>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {score.modules.map((m) => (
                    <ModuleTile key={m.code} module={m} />
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Right column — widgets */}
          <div className="lg:col-span-4 space-y-6">
            {/* Deadlines CTA */}
            <DeadlinesCta lang={lang} />

            {/* AI insights preview — Standard only */}
            {tier === "standard" && <InsightsCta />}

            {/* Upgrade card — Basic only */}
            {tier === "basic" && (
              <Card className="bg-[#ff4f00] text-white border-none shadow-md overflow-hidden relative group rounded-xl">
                <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-white/10 group-hover:scale-110 transition-transform duration-300" />
                <div className="absolute -left-12 -bottom-12 h-32 w-32 rounded-full bg-white/5" />
                <CardContent className="p-6 relative z-10 flex flex-col gap-4">
                  <div>
                    <h3 className="text-lg font-bold text-white leading-tight">{t.upgrade.title}</h3>
                    <p className="text-xs text-white/80 leading-relaxed mt-1.5">{t.upgrade.body}</p>
                  </div>
                  <Button
                    variant="secondary"
                    className="w-full bg-white text-[#ff4f00] hover:bg-white/95 rounded-xl font-bold h-10 transition-all hover:scale-[1.01]"
                  >
                    {t.upgrade.cta}
                  </Button>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
