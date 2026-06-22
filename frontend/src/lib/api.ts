/**
 * Thin fetch wrapper. Adds the in-memory access token, transparently refreshes once on a
 * 401, and normalises error responses (the backend ApiError envelope) into ApiError.
 */
let accessToken: string | null = null;
let refreshHandler: (() => Promise<string | null>) | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
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

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  allowRetry = true,
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;

  const res = await fetch(`/api${path}`, { ...options, headers });

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
