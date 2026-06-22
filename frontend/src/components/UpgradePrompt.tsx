import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useLang } from "@/lang/LanguageProvider";

interface UpgradePromptProps {
  /** "card" (default): full bordered Card. "inline": compact text+button row. */
  variant?: "card" | "inline";
}

/**
 * Reusable upgrade nudge shown to Basic users wherever Standard-only content is gated.
 * TODO: wire `cta` button to real upgrade/payment flow when billing is implemented.
 */
export function UpgradePrompt({ variant = "card" }: UpgradePromptProps) {
  const { t } = useLang();

  if (variant === "inline") {
    return (
      <div className="flex flex-wrap items-center gap-3 rounded-lg bg-muted/50 px-4 py-3">
        <Badge variant="outline" className="shrink-0 text-xs">
          {t.upgrade.tag}
        </Badge>
        <p className="flex-1 text-sm text-muted-foreground min-w-0">
          {t.upgrade.body}
        </p>
        {/* TODO: wire to payment flow */}
        <Button size="sm" variant="default" className="shrink-0">
          {t.upgrade.cta}
        </Button>
      </div>
    );
  }

  return (
    <Card className="border-primary/20 bg-primary/5">
      <CardContent className="pt-5 pb-4">
        <p className="text-sm font-semibold mb-1">{t.upgrade.title}</p>
        <p className="text-sm text-muted-foreground mb-3">{t.upgrade.body}</p>
        {/* TODO: wire to payment flow */}
        <Button size="sm" variant="default">{t.upgrade.cta}</Button>
      </CardContent>
    </Card>
  );
}
