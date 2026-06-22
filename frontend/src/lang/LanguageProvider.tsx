import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { en } from "./en";
import { sw } from "./sw";
import type { Dictionary, LanguageCode } from "./types";

const STORAGE_KEY = "riskily.lang";

const DICTIONARIES: Record<LanguageCode, unknown> = { en, sw };

/** Deep-merge a partial override onto the English base so missing keys fall back to English. */
function mergeWithFallback<T>(base: T, override: unknown): T {
  if (override == null) return base;
  if (typeof base !== "object" || base === null) return (override as T) ?? base;
  const out: Record<string, unknown> = Array.isArray(base) ? [...(base as unknown[])] as unknown as Record<string, unknown> : { ...(base as Record<string, unknown>) };
  const ov = override as Record<string, unknown>;
  for (const key of Object.keys(base as Record<string, unknown>)) {
    out[key] = mergeWithFallback((base as Record<string, unknown>)[key], ov[key]);
  }
  return out as T;
}

interface LanguageContextValue {
  lang: LanguageCode;
  setLang: (lang: LanguageCode) => void;
  /** Resolved dictionary for the active language (English-filled). */
  t: Dictionary;
}

const LanguageContext = createContext<LanguageContextValue | undefined>(undefined);

function readInitialLang(): LanguageCode {
  if (typeof window === "undefined") return "en";
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === "sw" ? "sw" : "en";
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<LanguageCode>(readInitialLang);

  useEffect(() => {
    if (typeof document !== "undefined") document.documentElement.lang = lang;
  }, [lang]);

  const setLang = (next: LanguageCode) => {
    setLangState(next);
    if (typeof window !== "undefined") window.localStorage.setItem(STORAGE_KEY, next);
    // TODO(api): when authenticated, persist to the user profile via PATCH /api/me { language }.
  };

  const value = useMemo<LanguageContextValue>(() => {
    const t = lang === "en" ? en : mergeWithFallback(en, DICTIONARIES[lang]);
    return { lang, setLang, t };
  }, [lang]);

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

/** Access the active language, the setter, and the resolved dictionary `t`. */
export function useLang(): LanguageContextValue {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error("useLang must be used within a LanguageProvider");
  return ctx;
}
