import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { apiFetch, setAccessToken, setRefreshHandler } from "@/lib/api";
import type { AuthResponse, LoginPayload, RegisterPayload, UserSummary } from "./types";

const REFRESH_KEY = "riskily.refresh";
const REMEMBER_KEY = "riskily.remember";

type Status = "loading" | "authenticated" | "anonymous";

interface AuthContextValue {
  status: Status;
  user: UserSummary | null;
  login: (payload: LoginPayload, remember?: boolean) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function storeRefresh(token: string | null, remember?: boolean) {
  if (typeof window === "undefined") return;
  if (remember !== undefined) {
    if (remember) window.localStorage.setItem(REMEMBER_KEY, "1");
    else window.localStorage.removeItem(REMEMBER_KEY);
  }
  const persistent = window.localStorage.getItem(REMEMBER_KEY) === "1";
  if (token) {
    if (persistent) {
      window.localStorage.setItem(REFRESH_KEY, token);
      window.sessionStorage.removeItem(REFRESH_KEY);
    } else {
      window.sessionStorage.setItem(REFRESH_KEY, token);
      window.localStorage.removeItem(REFRESH_KEY);
    }
  } else {
    window.localStorage.removeItem(REFRESH_KEY);
    window.sessionStorage.removeItem(REFRESH_KEY);
    window.localStorage.removeItem(REMEMBER_KEY);
  }
}

function readRefresh(): string | null {
  if (typeof window === "undefined") return null;
  return (
    window.localStorage.getItem(REFRESH_KEY) ||
    window.sessionStorage.getItem(REFRESH_KEY)
  );
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>("loading");
  const [user, setUser] = useState<UserSummary | null>(null);
  const refreshInFlight = useRef<Promise<string | null> | null>(null);

  const applyAuth = (auth: AuthResponse, remember?: boolean) => {
    setAccessToken(auth.accessToken);
    storeRefresh(auth.refreshToken, remember);
    setUser(auth.user);
    setStatus("authenticated");
  };

  const clearAuth = () => {
    setAccessToken(null);
    storeRefresh(null);
    setUser(null);
    setStatus("anonymous");
  };

  // Raw refresh (no apiFetch, to avoid recursion). Returns a new access token or null.
  const doRefresh = useCallback(async (): Promise<string | null> => {
    const refreshToken = readRefresh();
    if (!refreshToken) return null;
    if (refreshInFlight.current) return refreshInFlight.current;

    const run = (async () => {
      try {
        const res = await fetch("/api/auth/refresh", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        if (!res.ok) {
          clearAuth();
          return null;
        }
        const auth: AuthResponse = await res.json();
        applyAuth(auth); // no remember arg — re-reads stored preference via storeRefresh
        return auth.accessToken;
      } finally {
        refreshInFlight.current = null;
      }
    })();

    refreshInFlight.current = run;
    return run;
  }, []);

  // Register the refresh handler for the api client and bootstrap the session.
  useEffect(() => {
    setRefreshHandler(doRefresh);
    (async () => {
      if (!readRefresh()) {
        setStatus("anonymous");
        return;
      }
      const token = await doRefresh();
      if (!token) {
        setStatus("anonymous");
        return;
      }
      try {
        const me = await apiFetch<UserSummary>("/me");
        setUser(me);
        setStatus("authenticated");
      } catch {
        clearAuth();
      }
    })();
    return () => setRefreshHandler(null);
  }, [doRefresh]);

  const login = async (payload: LoginPayload, remember = false) => {
    const auth = await apiFetch<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    applyAuth(auth, remember);
  };

  const register = async (payload: RegisterPayload) => {
    const auth = await apiFetch<AuthResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    applyAuth(auth);
  };

  const logout = async () => {
    try {
      await apiFetch<void>("/auth/logout", { method: "POST" });
    } catch {
      // ignore — logout is best-effort on a stateless server
    }
    clearAuth();
  };

  const refreshUser = async () => {
    const me = await apiFetch<UserSummary>("/me");
    setUser(me);
  };

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, login, register, logout, refreshUser }),
    [status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
