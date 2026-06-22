import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { apiFetch } from "@/lib/api";
import { useLang } from "@/lang/LanguageProvider";

export default function ForgotPasswordPage() {
  const { t } = useLang();
  const [email, setEmail] = useState("");
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await apiFetch<void>("/auth/password/forgot", {
        method: "POST",
        body: JSON.stringify({ email }),
      });
    } catch {
      // Intentionally ignore — never reveal whether the email exists.
    } finally {
      setDone(true);
      setSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="font-display">{t.auth.forgot.title}</CardTitle>
          <CardDescription>{t.auth.forgot.subtitle}</CardDescription>
        </CardHeader>
        <CardContent>
          {done ? (
            <p className="text-sm text-muted-foreground">{t.auth.forgot.done}</p>
          ) : (
            <form className="flex flex-col gap-4" onSubmit={onSubmit}>
              <div className="flex flex-col gap-2">
                <Label htmlFor="email">{t.auth.emailLabel}</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              <Button type="submit" disabled={submitting}>
                {t.auth.forgot.submit}
              </Button>
            </form>
          )}
          <p className="mt-4 text-sm text-muted-foreground">
            <Link to="/login" className="font-semibold text-foreground underline-offset-4 hover:underline">
              {t.auth.forgot.backToLogin}
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
