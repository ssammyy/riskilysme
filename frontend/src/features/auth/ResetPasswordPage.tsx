import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { PasswordInput } from "@/components/PasswordInput";
import { apiFetch } from "@/lib/api";
import { useLang } from "@/lang/LanguageProvider";

export default function ResetPasswordPage() {
  const { t } = useLang();
  const [params] = useSearchParams();
  const token = params.get("token");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState<"idle" | "success" | "error">("idle");
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setSubmitting(true);
    try {
      await apiFetch<void>("/auth/password/reset", {
        method: "POST",
        body: JSON.stringify({ token, newPassword: password }),
      });
      setStatus("success");
    } catch {
      setStatus("error");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="font-display">{t.auth.reset.title}</CardTitle>
          <CardDescription>{t.auth.reset.subtitle}</CardDescription>
        </CardHeader>
        <CardContent>
          {!token ? (
            <p className="text-sm text-destructive">{t.auth.reset.missingToken}</p>
          ) : status === "success" ? (
            <p className="text-sm text-muted-foreground">{t.auth.reset.success}</p>
          ) : (
            <form className="flex flex-col gap-4" onSubmit={onSubmit}>
              <div className="flex flex-col gap-2">
                <Label htmlFor="password">{t.auth.reset.passwordLabel}</Label>
                <PasswordInput
                  id="password"
                  autoComplete="new-password"
                  required
                  minLength={8}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              {status === "error" && <p className="text-sm text-destructive">{t.auth.reset.error}</p>}
              <Button type="submit" disabled={submitting}>
                {t.auth.reset.submit}
              </Button>
            </form>
          )}
          <p className="mt-4 text-sm text-muted-foreground">
            <Link to="/login" className="font-semibold text-foreground underline-offset-4 hover:underline">
              {t.auth.reset.toLogin}
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
