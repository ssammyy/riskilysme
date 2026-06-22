import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { UpgradePrompt } from "@/components/UpgradePrompt";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";
import { apiFetch } from "@/lib/api";
import type { BandCode, ModuleCode, ScoreResponse } from "@/score/types";

const BAND_VARIANT: Record<BandCode, "good" | "watch" | "act" | "urgent"> = {
  all_good: "good",
  watch_out: "watch",
  act_now: "act",
  urgent: "urgent",
};

function toModuleCode(raw: string): ModuleCode | null {
  const upper = raw.toUpperCase() as ModuleCode;
  const valid: ModuleCode[] = ["FX", "LIQUIDITY", "COUNTERPARTY", "COMMODITY", "CREDIT", "REGULATORY", "MACRO"];
  return valid.includes(upper) ? upper : null;
}

export default function ModuleDetailPage() {
  const { code: rawCode } = useParams<{ code: string }>();
  const { user } = useAuth();
  const { t } = useLang();

  const code = rawCode ? toModuleCode(rawCode) : null;

  const { data, isLoading } = useQuery({
    queryKey: ["score", "me"],
    queryFn: () => apiFetch<ScoreResponse>("/score/me"),
    retry: false,
  });

  if (!code) {
    return (
      <main className="min-h-screen bg-background px-4 py-10 sm:px-6">
        <p className="text-muted-foreground">{t.modules.notFound}</p>
      </main>
    );
  }

  const module = data?.modules.find((m) => m.code === code);

  const name = t.modules.names[code] ?? code;
  const description = t.modules.descriptions[code];
  const isStandard = user?.subscriptionTier === "standard";

  return (
    <main className="min-h-screen bg-background px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-2xl flex-col gap-6">
        {/* Back link */}
        <Button variant="outline" size="sm" asChild className="self-start">
          <Link to="/">{t.modules.backToScore}</Link>
        </Button>

        {/* Header */}
        <div className="flex flex-col gap-1">
          <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground">
            {t.dashboard.modulesTitle}
          </p>
          <h1 className="font-display text-3xl font-medium tracking-tight text-foreground sm:text-4xl">
            {name}
          </h1>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>

        <Separator />

        {/* Score card */}
        {isLoading && (
          <Card>
            <CardContent className="py-6">
              <div className="h-16 animate-pulse rounded-md bg-muted" />
            </CardContent>
          </Card>
        )}

        {module && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-3">
                <span className="font-display text-4xl font-semibold text-foreground">
                  {module.health}
                </span>
                <Badge variant={BAND_VARIANT[module.band]}>
                  {t.score.band[module.band]}
                </Badge>
                {module.isProvisional && (
                  <Badge variant="outline">{t.score.provisional}</Badge>
                )}
              </CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-5">
              <Progress value={module.health} className="h-2" />

              {/* Standard-only detail */}
              {isStandard ? (
                <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                  <StatBox label={t.score.detail.health} value={`${module.health}/100`} />
                  <StatBox
                    label={t.score.detail.exposure}
                    value={(module.exposure * 100).toFixed(1) + "%"}
                  />
                  <StatBox
                    label={t.score.detail.pressure}
                    value={module.pressure.toFixed(2)}
                  />
                  <StatBox
                    label={t.score.dataConfidence.profile}
                    value={t.score.dataConfidence[module.dataConfidence]}
                  />
                </div>
              ) : (
                <UpgradePrompt variant="inline" />
              )}
            </CardContent>
          </Card>
        )}

        {/* Action recommendation */}
        {module && (
          <Card className="border-0 bg-muted/40">
            <CardContent className="py-5">
              <p className="text-sm leading-relaxed text-muted-foreground sm:text-base">
                {t.modules.actions[code]}
              </p>
            </CardContent>
          </Card>
        )}
      </div>
    </main>
  );
}

function StatBox({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className="font-display text-lg font-semibold text-foreground">{value}</span>
    </div>
  );
}
