/**
 * Thin fetch wrapper. Proactively refreshes the access token when it is expired or
 * within 30 s of expiry, before sending the request. Falls back to a reactive retry
 * on 401 in case of a race between expiry and clock skew.
 */
let accessToken: string | null = null;
let accessTokenExp: number | null = null; // unix epoch seconds from JWT exp claim
let refreshHandler: (() => Promise<string | null>) | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
  if (token) {
    try {
      const payload = JSON.parse(
        atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")),
      );
      accessTokenExp = typeof payload.exp === "number" ? payload.exp : null;
    } catch {
      accessTokenExp = null;
    }
  } else {
    accessTokenExp = null;
  }
}

export function setRefreshHandler(fn: (() => Promise<string | null>) | null) {
  refreshHandler = fn;
}

export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;
  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body = await res.json();
    return new ApiError(res.status, body.message ?? res.statusText, body.fieldErrors);
  } catch {
    return new ApiError(res.status, res.statusText);
  }
}

function tokenNeedsRefresh(): boolean {
  if (!accessToken) return true;
  if (accessTokenExp === null) return false; // can't determine — assume valid
  return Date.now() / 1000 > accessTokenExp - 30; // refresh 30 s before expiry
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  allowRetry = true,
): Promise<T> {
  // Proactively refresh before the request if the token is stale
  if (allowRetry && refreshHandler && tokenNeedsRefresh()) {
    const next = await refreshHandler();
    if (next) accessToken = next;
  }

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;

  const res = await fetch(`/api${path}`, { ...options, headers });

  // Reactive fallback in case of clock skew or a concurrent token rotation
  if (res.status === 401 && allowRetry && refreshHandler) {
    const next = await refreshHandler();
    if (next) {
      accessToken = next;
      return apiFetch<T>(path, options, false);
    }
  }

  if (!res.ok) throw await toApiError(res);
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}
