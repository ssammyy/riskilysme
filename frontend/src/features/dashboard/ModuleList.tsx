import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { useLang } from "@/lang/LanguageProvider";
import type { BandCode, ModuleCode, ModuleScore } from "@/score/types";

interface ModuleListProps {
  modules: ModuleScore[];
  subscriptionTier: "basic" | "standard";
}

const BAND_VARIANT: Record<BandCode, "good" | "watch" | "act" | "urgent"> = {
  all_good: "good",
  watch_out: "watch",
  act_now: "act",
  urgent: "urgent",
};

export function ModuleList({ modules, subscriptionTier }: ModuleListProps) {
  const { t } = useLang();

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {modules.map((m) => {
        const code = m.code as ModuleCode;
        const name = t.modules.names[code] ?? code;
        const bandLabel = t.score.band[m.band];

        return (
          <Link
            key={m.code}
            to={`/modules/${m.code.toLowerCase()}`}
            className="block rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring transition-transform hover:scale-[1.01]"
          >
            <Card className="h-full border border-border/45 bg-card/25 shadow-sm transition-all hover:bg-card hover:shadow-md">
              <CardContent className="flex flex-col gap-4 p-5 h-full justify-between">
                {/* Module title & Score */}
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-[#201515] leading-tight truncate">
                        {name}
                      </span>
                      {m.isProvisional && (
                        <Badge variant="outline" className="text-[10px] font-semibold text-muted-foreground bg-white border-border/50 rounded px-1 py-0">
                          {t.score.provisional}
                        </Badge>
                      )}
                    </div>
                    <p className="text-[11px] text-muted-foreground mt-1 line-clamp-1">
                      {t.modules.descriptions[code] || ""}
                    </p>
                  </div>
                  <span className="font-display text-2xl font-extrabold text-[#201515] leading-none shrink-0">
                    {m.health}
                  </span>
                </div>

                {/* Progress bar — Standard only */}
                {subscriptionTier === "standard" && (
                  <div className="space-y-1">
                    <Progress value={m.health} className="h-1 bg-border/45" />
                  </div>
                )}

                {/* Severity Badge & Details link */}
                <div className="flex items-center justify-between pt-1 border-t border-border/30">
                  <Badge variant={BAND_VARIANT[m.band]} className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider">
                    {bandLabel}
                  </Badge>
                  <span className="text-xs font-semibold text-primary group-hover:underline">
                    {t.modules.seeDetail} &rarr;
                  </span>
                </div>
              </CardContent>
            </Card>
          </Link>
        );
      })}
    </div>
  );
}
