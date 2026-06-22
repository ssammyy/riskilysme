import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useLang } from "@/lang/LanguageProvider";
import type { BandCode } from "@/score/types";

interface ScoreGaugeProps {
  score: number;
  band: BandCode;
  calculatedAt: string;
  /** "basic" users see weekly cadence label; "standard" users see daily. */
  subscriptionTier: "basic" | "standard";
}

const BAND_VARIANT: Record<BandCode, "good" | "watch" | "act" | "urgent"> = {
  all_good: "good",
  watch_out: "watch",
  act_now: "act",
  urgent: "urgent",
};

/** Stroke colour per band — uses Tailwind CSS variable tokens to stay on-theme. */
const ARC_COLOR: Record<BandCode, string> = {
  all_good: "hsl(var(--band-good))",
  watch_out: "hsl(var(--band-watch))",
  act_now: "hsl(var(--band-act))",
  urgent: "hsl(var(--band-urgent))",
};

function toArcPath(score: number): string {
  // Circle centred at (60, 60), r = 48, arc spans 270° (−135° to +135° from bottom)
  const R = 48;
  const cx = 60;
  const cy = 60;
  const startAngle = -225; // degrees, measured clockwise from positive-x axis
  const sweepAngle = 270;
  const fraction = Math.min(Math.max(score, 0), 100) / 100;
  const endAngle = startAngle + sweepAngle * fraction;

  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const x1 = cx + R * Math.cos(toRad(startAngle));
  const y1 = cy + R * Math.sin(toRad(startAngle));
  const x2 = cx + R * Math.cos(toRad(endAngle));
  const y2 = cy + R * Math.sin(toRad(endAngle));
  const largeArc = sweepAngle * fraction > 180 ? 1 : 0;

  return `M ${x1} ${y1} A ${R} ${R} 0 ${largeArc} 1 ${x2} ${y2}`;
}

function toTrackPath(): string {
  const R = 48;
  const cx = 60;
  const cy = 60;
  const startAngle = -225;
  const endAngle = startAngle + 270;
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const x1 = cx + R * Math.cos(toRad(startAngle));
  const y1 = cy + R * Math.sin(toRad(startAngle));
  const x2 = cx + R * Math.cos(toRad(endAngle));
  const y2 = cy + R * Math.sin(toRad(endAngle));
  return `M ${x1} ${y1} A ${R} ${R} 0 1 1 ${x2} ${y2}`;
}

function isThisWeek(isoString: string): boolean {
  const date = new Date(isoString);
  const now = new Date();
  const startOfWeek = new Date(now);
  startOfWeek.setDate(now.getDate() - now.getDay()); // Sunday
  startOfWeek.setHours(0, 0, 0, 0);
  return date >= startOfWeek;
}

function isToday(isoString: string): boolean {
  const date = new Date(isoString);
  const now = new Date();
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  );
}

export function ScoreGauge({ score, band, calculatedAt, subscriptionTier }: ScoreGaugeProps) {
  const { t } = useLang();

  const updatedLabel =
    subscriptionTier === "standard"
      ? isToday(calculatedAt)
        ? t.dashboard.updatedToday
        : t.dashboard.updatedThisWeek
      : isThisWeek(calculatedAt)
        ? t.dashboard.updatedThisWeek
        : t.dashboard.updatedThisWeek;

  const arcColor = ARC_COLOR[band];
  const bandLabel = t.score.band[band];
  const bandDescription = t.score.bandDescription[band];

  return (
    <Card>
      <CardContent className="flex flex-col items-center gap-4 py-6 sm:flex-row sm:items-center sm:gap-8 sm:py-8">
        {/* SVG Gauge */}
        <div className="relative flex shrink-0 items-center justify-center" aria-hidden="true">
          <svg width="120" height="120" viewBox="0 0 120 120">
            {/* Track */}
            <path
              d={toTrackPath()}
              fill="none"
              stroke="hsl(var(--muted))"
              strokeWidth="8"
              strokeLinecap="round"
            />
            {/* Arc */}
            {score > 0 && (
              <path
                d={toArcPath(score)}
                fill="none"
                stroke={arcColor}
                strokeWidth="8"
                strokeLinecap="round"
              />
            )}
          </svg>
          {/* Score label centred inside gauge */}
          <div className="absolute flex flex-col items-center">
            <span className="font-display text-3xl font-semibold leading-none text-foreground">
              {score}
            </span>
            <span className="text-xs text-muted-foreground">/100</span>
          </div>
        </div>

        {/* Text info */}
        <div className="flex flex-col gap-2 text-center sm:text-left">
          <p className="text-sm font-medium uppercase tracking-wide text-muted-foreground">
            {t.score.overallHealth}
          </p>
          <Badge variant={BAND_VARIANT[band]} className="self-center sm:self-start">
            {bandLabel}
          </Badge>
          <p className="max-w-xs text-sm text-muted-foreground">{bandDescription}</p>
          <p className="text-xs text-muted-foreground">{updatedLabel}</p>
          {/* Basic-tier nudge: upgrade for daily score updates */}
          {subscriptionTier === "basic" && (
            <Button
              size="sm"
              variant="outline"
              className="self-center sm:self-start text-xs mt-1"
              // TODO: wire to upgrade/payment flow
            >
              {t.upgrade.cta}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
