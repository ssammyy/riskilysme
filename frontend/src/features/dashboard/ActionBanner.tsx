import { Card, CardContent } from "@/components/ui/card";
import { useLang } from "@/lang/LanguageProvider";
import type { ModuleCode, ModuleScore } from "@/score/types";

interface ActionBannerProps {
  firstName: string | null;
  modules: ModuleScore[];
}

function getGreeting(t: ReturnType<typeof useLang>["t"]): string {
  const hour = new Date().getHours();
  if (hour < 12) return t.dashboard.greeting.morning;
  if (hour < 17) return t.dashboard.greeting.afternoon;
  return t.dashboard.greeting.evening;
}

function worstModule(modules: ModuleScore[]): ModuleScore | null {
  if (modules.length === 0) return null;
  return modules.reduce((worst, m) => (m.health < worst.health ? m : worst), modules[0]);
}

export function ActionBanner({ firstName, modules }: ActionBannerProps) {
  const { t } = useLang();
  const greeting = getGreeting(t);
  const name = firstName ? `, ${firstName}` : "";
  const worst = worstModule(modules);
  const action = worst ? t.modules.actions[worst.code as ModuleCode] : null;

  return (
    <Card className="border-0 bg-muted/40">
      <CardContent className="flex flex-col gap-2 py-5 sm:py-6">
        <h1 className="font-display text-2xl font-medium tracking-tight text-foreground sm:text-3xl">
          {greeting}{name}.
        </h1>
        {action && (
          <p className="max-w-prose text-sm leading-relaxed text-muted-foreground sm:text-base">
            {action}
          </p>
        )}
      </CardContent>
    </Card>
  );
}
