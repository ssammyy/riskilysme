import { useQuery } from "@tanstack/react-query";
import { Navigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";
import { apiFetch } from "@/lib/api";
import type { ScoreResponse } from "@/score/types";
import { ActionBanner } from "./ActionBanner";
import { DeadlinesGrid } from "./DeadlinesGrid";
import { MarketDataStrip } from "./MarketDataStrip";
import { ScoreGauge } from "./ScoreGauge";
import { ModuleList } from "./ModuleList";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function DashboardPage() {
  const { t, lang } = useLang();
  const { user } = useAuth();

  const { data: score, isError, error } = useQuery({
    queryKey: ["score", "me"],
    queryFn: () => apiFetch<ScoreResponse>("/score/me"),
    retry: false,
  });

  // Defensive: 404 means onboarding incomplete — redirect
  const apiError = error as { status?: number } | null;
  if (isError && apiError?.status === 404) {
    return <Navigate to="/onboarding" replace />;
  }

  const tier = user?.subscriptionTier ?? "basic";

  return (
    <main className="min-h-screen bg-background px-6 py-8 sm:px-8">
      <div className="mx-auto max-w-7xl flex flex-col gap-6">
        {/* Header Row */}
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

        {/* 12-Column Grid Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Left Column (col-span-8): Primary Stats & Risk Breakdown */}
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

            {/* Market data strip — 5 live market signals */}
            <MarketDataStrip />

            {score && score.modules.length > 0 && (
              <div className="space-y-4">
                <div className="flex items-center justify-between border-b border-border/30 pb-2">
                  <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    {t.dashboard.modulesTitle}
                  </h2>
                </div>
                <ModuleList modules={score.modules} subscriptionTier={tier} />
              </div>
            )}
          </div>

          {/* Right Column (col-span-4): Compliance Deadlines & Widgets */}
          <div className="lg:col-span-4 space-y-6">
            <Card className="border border-border/40 shadow-sm overflow-hidden bg-card/25">
              <CardContent className="p-6">
                <DeadlinesGrid subscriptionTier={tier} />
              </CardContent>
            </Card>

            {/* standard plan upsell / active alerts summary widget */}
            {tier === "basic" && (
              <Card className="bg-[#ff4f00] text-white border-none shadow-md overflow-hidden relative group rounded-xl">
                {/* Subtle decorative circles */}
                <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-white/10 group-hover:scale-110 transition-transform duration-300" />
                <div className="absolute -left-12 -bottom-12 h-32 w-32 rounded-full bg-white/5" />
                
                <CardContent className="p-6 relative z-10 flex flex-col gap-4">
                  <div>
                    <h3 className="text-lg font-bold text-white leading-tight">
                      {t.upgrade.title}
                    </h3>
                    <p className="text-xs text-white/80 leading-relaxed mt-1.5">
                      {t.upgrade.body}
                    </p>
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
