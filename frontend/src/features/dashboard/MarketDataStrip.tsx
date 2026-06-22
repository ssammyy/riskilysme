import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { apiFetch } from "@/lib/api";
import { useLang } from "@/lang/LanguageProvider";

interface MarketItem {
  current: number;
  prev: number;
  delta: number;
  direction: "UP" | "DOWN" | "STABLE";
}

interface MarketDataResponse {
  snapshotDate: string;
  usdkes: MarketItem;
  fuel: MarketItem;
  cbkRate: MarketItem;
  unga: MarketItem;
  kraDeadlineDays: number;
  refreshedAt: string;
}

type StripVariant = "good" | "watch" | "urgent" | "outline";

interface StripItem {
  label: string;
  value: string;
  badge: string;
  variant: StripVariant;
}

function directionVariant(direction: "UP" | "DOWN" | "STABLE"): StripVariant {
  if (direction === "UP") return "watch";
  if (direction === "DOWN") return "good";
  return "outline";
}

export function MarketDataStrip() {
  const { t } = useLang();
  const md = t.marketData;

  const { data, isLoading } = useQuery<MarketDataResponse | null>({
    queryKey: ["market-data", "latest"],
    queryFn: () => apiFetch<MarketDataResponse | null>("/market-data/latest"),
    staleTime: 1000 * 60 * 60, // 1 hour — refreshes once per day
  });

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3">
        <h2 className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          {md.title}
        </h2>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-5">
          {[1, 2, 3, 4, 5].map((n) => (
            <div key={n} className="h-[72px] w-full animate-pulse rounded-xl bg-muted" />
          ))}
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex flex-col gap-3">
        <h2 className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          {md.title}
        </h2>
        <p className="text-sm text-muted-foreground">{md.noData}</p>
      </div>
    );
  }

  const days = data.kraDeadlineDays;
  const kraCountdown =
    days === 0
      ? md.kraCountdown.today
      : days === 1
        ? md.kraCountdown.tomorrow
        : `${days} ${md.kraCountdown.days}`;
  const kraVariant: StripVariant = days <= 3 ? "urgent" : days <= 7 ? "watch" : "outline";
  const kraImpact =
    days <= 3
      ? md.kraImpact.urgent
      : days <= 7
        ? md.kraImpact.comingUp
        : md.kraImpact.onTrack;

  const items: StripItem[] = [
    {
      label: md.items.usdkes,
      value: data.usdkes.current.toFixed(1),
      badge: md.direction[data.usdkes.direction],
      variant: directionVariant(data.usdkes.direction),
    },
    {
      label: md.items.fuel,
      value: `KES ${Math.round(data.fuel.current)}`,
      badge: md.direction[data.fuel.direction],
      variant: directionVariant(data.fuel.direction),
    },
    {
      label: md.items.cbkRate,
      value: `${data.cbkRate.current.toFixed(2)}%`,
      badge: md.direction[data.cbkRate.direction],
      variant: directionVariant(data.cbkRate.direction),
    },
    {
      label: md.items.unga,
      value: `KES ${Math.round(data.unga.current)}`,
      badge: md.direction[data.unga.direction],
      variant: directionVariant(data.unga.direction),
    },
    {
      label: md.items.kraDeadline,
      value: kraCountdown,
      badge: kraImpact,
      variant: kraVariant,
    },
  ];

  return (
    <div className="flex flex-col gap-3">
      <h2 className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
        {md.title}
      </h2>
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-5">
        {items.map((item) => (
          <Card key={item.label} className="border-muted">
            <CardContent className="px-3 py-3">
              <p className="truncate text-xs text-muted-foreground">{item.label}</p>
              <p className="mt-1 text-sm font-semibold">{item.value}</p>
              <Badge variant={item.variant} className="mt-1.5 text-xs">
                {item.badge}
              </Badge>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
