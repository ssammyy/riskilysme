import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { MailCheck, ShieldCheck, AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";

type VerifyState = "idle" | "verifying" | "verified" | "link-error";
type ResendState = "idle" | "sent" | "cooldown";

const RESEND_COOLDOWN_MS = 60_000;

export default function VerifyEmailPage() {
  const { t } = useLang();
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const token = params.get("token");

  const [verifyState, setVerifyState] = useState<VerifyState>(token ? "verifying" : "idle");
  const [resendState, setResendState] = useState<ResendState>("idle");
  const [cooldownLeft, setCooldownLeft] = useState(0);
  const cooldownRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const didVerify = useRef(false);

  // Auto-consume the token from the email link.
  useEffect(() => {
    if (!token || didVerify.current) return;
    didVerify.current = true;

    apiFetch<void>("/auth/email/verify", {
      method: "POST",
      body: JSON.stringify({ token }),
    })
      .then(async () => {
        setVerifyState("verified");
        // Refresh the user object so emailVerified becomes true in AuthProvider.
        try { await refreshUser(); } catch { /* best-effort */ }
        setTimeout(() => navigate("/", { replace: true }), 2000);
      })
      .catch(() => setVerifyState("link-error"));
  }, [token, navigate, refreshUser]);

  // When this page is loaded without a token (redirected from ProtectedRoute),
  // automatically trigger a verification email so the user doesn't have to click anything.
  useEffect(() => {
    if (token || !user || resendState !== "idle") return;
    doResend();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const doResend = async () => {
    if (!user) return;
    setResendState("sent");
    try {
      await apiFetch<void>("/auth/email/resend", {
        method: "POST",
        body: JSON.stringify({ email: user.email }),
      });
    } catch {
      // server always returns 204 — this only fires on network errors
    }
    startCooldown();
  };

  const startCooldown = () => {
    setResendState("cooldown");
    setCooldownLeft(RESEND_COOLDOWN_MS / 1000);
    cooldownRef.current = setInterval(() => {
      setCooldownLeft((s) => {
        if (s <= 1) {
          clearInterval(cooldownRef.current!);
          setResendState("idle");
          return 0;
        }
        return s - 1;
      });
    }, 1000);
  };

  // ── Token-based states ─────────────────────────────────────────────────────

  if (verifyState === "verifying") {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
        <Card className="w-full max-w-md text-center">
          <CardContent className="flex flex-col items-center gap-4 py-10">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-muted border-t-primary" />
            <p className="text-sm text-muted-foreground">{t.auth.verify.verifying}</p>
          </CardContent>
        </Card>
      </main>
    );
  }

  if (verifyState === "verified") {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
        <Card className="w-full max-w-md text-center">
          <CardContent className="flex flex-col items-center gap-4 py-10">
            <ShieldCheck className="h-12 w-12 text-green-500" strokeWidth={1.5} />
            <div>
              <p className="font-display text-lg font-semibold text-foreground">
                {t.auth.verify.successTitle}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">{t.auth.verify.successSub}</p>
            </div>
          </CardContent>
        </Card>
      </main>
    );
  }

  if (verifyState === "link-error") {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
        <Card className="w-full max-w-md">
          <CardHeader>
            <div className="flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" />
              <CardTitle className="font-display">{t.auth.verify.errorTitle}</CardTitle>
            </div>
            <CardDescription>{t.auth.verify.errorSub}</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            {user && (
              <Button
                onClick={doResend}
                disabled={resendState === "cooldown"}
              >
                {resendState === "cooldown"
                  ? `${t.auth.verify.resendCooldown} (${cooldownLeft}s)`
                  : resendState === "sent"
                  ? t.auth.verify.resendDone
                  : t.auth.verify.resend}
              </Button>
            )}
            <p className="text-sm text-muted-foreground">
              <Link to="/login" className="font-semibold text-foreground underline-offset-4 hover:underline">
                {t.auth.verify.backToLogin}
              </Link>
            </p>
          </CardContent>
        </Card>
      </main>
    );
  }

  // ── Default: "check your inbox" state ─────────────────────────────────────

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center gap-3">
            <MailCheck className="h-6 w-6 text-primary shrink-0" strokeWidth={1.5} />
            <CardTitle className="font-display">{t.auth.verify.title}</CardTitle>
          </div>
          <CardDescription>{t.auth.verify.subtitle}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-5">
          <div className="rounded-lg border border-border/40 bg-muted/30 px-4 py-3">
            <p className="text-xs text-muted-foreground">{t.auth.verify.checkInbox}</p>
            <p className="mt-0.5 text-sm font-semibold text-foreground break-all">
              {user?.email ?? "—"}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">{t.auth.verify.checkInboxSub}</p>
          </div>

          {resendState === "sent" && (
            <p className="text-sm text-green-600">{t.auth.verify.resendDone}</p>
          )}

          <Button
            variant="outline"
            onClick={doResend}
            disabled={resendState === "cooldown"}
          >
            {resendState === "cooldown"
              ? `${t.auth.verify.resendCooldown} (${cooldownLeft}s)`
              : t.auth.verify.resend}
          </Button>

          <p className="text-sm text-muted-foreground">
            <Link to="/login" className="font-semibold text-foreground underline-offset-4 hover:underline">
              {t.auth.verify.backToLogin}
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
