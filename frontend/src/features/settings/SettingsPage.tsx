import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PasswordInput } from "@/components/PasswordInput";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";
import { apiFetch } from "@/lib/api";
import type { UserSummary } from "@/auth/types";
import type { LanguageCode } from "@/lang/types";

export default function SettingsPage() {
  const { t, lang, setLang } = useLang();
  const { user, refreshUser } = useAuth();

  const [firstName, setFirstName] = useState(user?.firstName ?? "");
  const [businessName, setBusinessName] = useState("");
  const [language, setLanguage] = useState<LanguageCode>(user?.language ?? lang);
  const [profileSaved, setProfileSaved] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [pwStatus, setPwStatus] = useState<"idle" | "success" | "error">("idle");

  const saveProfile = async (e: FormEvent) => {
    e.preventDefault();
    setProfileSaved(false);
    await apiFetch<UserSummary>("/me", {
      method: "PATCH",
      body: JSON.stringify({ firstName, businessName, language }),
    });
    setLang(language);
    await refreshUser();
    setProfileSaved(true);
  };

  const changePassword = async (e: FormEvent) => {
    e.preventDefault();
    setPwStatus("idle");
    try {
      await apiFetch<void>("/me/change-password", {
        method: "POST",
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      setCurrentPassword("");
      setNewPassword("");
      setPwStatus("success");
    } catch {
      setPwStatus("error");
    }
  };

  return (
    <main className="min-h-screen bg-background px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-2xl flex-col gap-8">
        <header>
          <h1 className="font-display text-3xl font-medium tracking-tight text-foreground">
            {t.settings.title}
          </h1>
        </header>

        <Card>
          <CardHeader>
            <CardTitle>{t.settings.profileTitle}</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="flex flex-col gap-4" onSubmit={saveProfile}>
              <div className="flex flex-col gap-2">
                <Label htmlFor="firstName">{t.settings.firstNameLabel}</Label>
                <Input id="firstName" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="businessName">{t.settings.businessNameLabel}</Label>
                <Input
                  id="businessName"
                  value={businessName}
                  onChange={(e) => setBusinessName(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="language">{t.settings.languageLabel}</Label>
                <select
                  id="language"
                  value={language}
                  onChange={(e) => setLanguage(e.target.value as LanguageCode)}
                  className="h-11 rounded-md border border-foreground/40 bg-background px-4 text-base text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <option value="en">English</option>
                  <option value="sw">Kiswahili</option>
                </select>
              </div>
              {profileSaved && <p className="text-sm text-band-good">{t.settings.profileSaved}</p>}
              <div>
                <Button type="submit">{t.settings.saveProfile}</Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t.settings.passwordTitle}</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="flex flex-col gap-4" onSubmit={changePassword}>
              <div className="flex flex-col gap-2">
                <Label htmlFor="currentPassword">{t.settings.currentPasswordLabel}</Label>
                <PasswordInput
                  id="currentPassword"
                  autoComplete="current-password"
                  required
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="newPassword">{t.settings.newPasswordLabel}</Label>
                <PasswordInput
                  id="newPassword"
                  autoComplete="new-password"
                  required
                  minLength={8}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </div>
              {pwStatus === "success" && (
                <p className="text-sm text-band-good">{t.settings.passwordChanged}</p>
              )}
              {pwStatus === "error" && (
                <p className="text-sm text-destructive">{t.settings.passwordError}</p>
              )}
              <div>
                <Button type="submit">{t.settings.changePassword}</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
