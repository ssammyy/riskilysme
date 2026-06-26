import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { PasswordInput } from "@/components/PasswordInput";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";

export default function LoginPage() {
  const { t } = useLang();
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(false);
    setSubmitting(true);
    try {
      await login({ email, password }, remember);
      navigate("/", { replace: true });
    } catch {
      setError(true);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="font-display">{t.auth.login.title}</CardTitle>
          <CardDescription>{t.auth.login.subtitle}</CardDescription>
        </CardHeader>
        <CardContent>
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
            <div className="flex flex-col gap-2">
              <Label htmlFor="password">{t.auth.passwordLabel}</Label>
              <PasswordInput
                id="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <div className="flex items-center gap-2">
              <Checkbox
                id="remember"
                checked={remember}
                onCheckedChange={(v) => setRemember(v === true)}
              />
              <Label htmlFor="remember" className="cursor-pointer font-normal text-sm">
                {t.auth.login.rememberMe}
              </Label>
            </div>
            {error && <p className="text-sm text-destructive">{t.auth.login.error}</p>}
            <Button type="submit" disabled={submitting}>
              {t.auth.login.submit}
            </Button>
          </form>
          <p className="mt-3 text-sm">
            <Link to="/forgot" className="text-muted-foreground underline-offset-4 hover:underline">
              {t.auth.forgot.link}
            </Link>
          </p>
          <p className="mt-4 text-sm text-muted-foreground">
            {t.auth.login.noAccount}{" "}
            <Link to="/register" className="font-semibold text-foreground underline-offset-4 hover:underline">
              {t.auth.login.signUpLink}
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
