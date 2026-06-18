import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

interface HealthResponse {
  status: string;
  service: string;
  time: string;
}

async function fetchHealth(): Promise<HealthResponse> {
  const res = await fetch("/api/health");
  if (!res.ok) throw new Error(`Health check failed: ${res.status}`);
  return res.json();
}

/**
 * Sprint 1 design-language proof page. Renders DESIGN.md-themed shadcn primitives and
 * pulls the real backend health endpoint (no hardcoded/fake data). Replaced by the
 * dashboard in Sprint 3.
 */
export default function App() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["health"],
    queryFn: fetchHealth,
    retry: false,
  });

  return (
    <main className="min-h-screen bg-background px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-3xl flex-col gap-8">
        <header className="flex flex-col gap-2">
          <span className="text-sm font-medium uppercase tracking-[1px] text-foreground">
            Riskily SME
          </span>
          <h1 className="font-display text-4xl font-medium tracking-tight text-foreground sm:text-5xl">
            Risk intelligence for your business
          </h1>
          <p className="text-lg text-muted-foreground">
            Foundation scaffold — design language, theming, and the live backend connection.
          </p>
        </header>

        <Card>
          <CardHeader>
            <CardTitle>Backend connection</CardTitle>
            <CardDescription>Live status from <code>GET /api/health</code></CardDescription>
          </CardHeader>
          <CardContent className="flex items-center gap-3">
            {isLoading && <Badge>Checking…</Badge>}
            {isError && <Badge variant="urgent">Backend unreachable</Badge>}
            {data && (
              <>
                <Badge variant="good">{data.status}</Badge>
                <span className="text-sm text-muted-foreground">
                  {data.service} · {new Date(data.time).toLocaleTimeString()}
                </span>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Score bands</CardTitle>
            <CardDescription>Business Health Score banding (methodology §2)</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            <Badge variant="good">All Good</Badge>
            <Badge variant="watch">Watch Out</Badge>
            <Badge variant="act">Act Now</Badge>
            <Badge variant="urgent">Urgent</Badge>
          </CardContent>
        </Card>

        <div className="flex flex-wrap gap-3">
          <Button>Get my score</Button>
          <Button variant="secondary">Sign in</Button>
          <Button variant="outline">Learn more</Button>
        </div>
      </div>
    </main>
  );
}
