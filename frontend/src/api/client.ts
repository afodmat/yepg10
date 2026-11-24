/** Central HTTP client. Auth lives in httpOnly cookies set by the backend; JS never sees tokens. */
const BASE = import.meta.env.VITE_API_URL ?? "/api";

export class ApiError extends Error {
  constructor(public status: number, message: string, public fieldErrors?: Record<string, string>) { super(message); }
}

function xsrf(): Record<string, string> {
  const cookie = document.cookie.split(";").map((c) => c.trim()).find((c) => c.startsWith("XSRF-TOKEN="));
  const token = cookie?.slice("XSRF-TOKEN=".length);
  return token ? { "X-XSRF-TOKEN": decodeURIComponent(token) } : {};
}

async function ensureXsrfToken(): Promise<void> {
  if (xsrf()["X-XSRF-TOKEN"]) return;
  const res = await fetch(`${BASE}/auth/csrf`, { method: "GET", credentials: "include", headers: { Accept: "application/json" } });
  if (!res.ok || !xsrf()["X-XSRF-TOKEN"]) {
    throw new ApiError(500, "Could not initialize the security token. Refresh the page and try again.");
  }
}

let refreshing: Promise<boolean> | null = null;
function tryRefresh(): Promise<boolean> {
  refreshing ??= fetch(`${BASE}/auth/refresh`, { method: "POST", credentials: "include", headers: xsrf() })
    .then((r) => r.ok).catch(() => false).finally(() => { refreshing = null; });
  return refreshing;
}

async function request<T>(path: string, init: RequestInit, retry = true): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const csrfExempt = ["/auth/register", "/auth/login", "/auth/forgot-password", "/auth/reset-password"].includes(path);
  if (!["GET", "HEAD", "OPTIONS"].includes(method) && !csrfExempt) await ensureXsrfToken();
  const res = await fetch(`${BASE}${path}`, { ...init, credentials: "include", headers: { Accept: "application/json", ...xsrf(), ...(init.headers as Record<string, string>) } });
  if (res.status === 401 && retry && !path.startsWith("/auth/")) {
    if (await tryRefresh()) return request<T>(path, init, false);
  }
  if (res.status === 204) return undefined as T;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new ApiError(res.status, data.message ?? "Something went wrong. Please try again.", data.fieldErrors);
  return data as T;
}

/** Builds "?a=1&b=2", skipping empty values. */
export const qs = (params: Record<string, string | number | boolean | undefined | null>) => {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== "") p.set(k, String(v)); });
  const s = p.toString();
  return s ? `?${s}` : "";
};

export const http = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: "POST", headers: body === undefined ? {} : { "Content-Type": "application/json" }, body: body === undefined ? undefined : JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body ?? {}) }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body ?? {}) }),
  del: <T>(path: string) => request<T>(path, { method: "DELETE" }),
  upload: <T>(path: string, form: FormData) => request<T>(path, { method: "POST", body: form }),
};
// Back-compat for modules written against the first version
export const api = <T,>(path: string, o: { method?: string; body?: unknown } = {}) => request<T>(path, { method: o.method ?? "GET", headers: o.body ? { "Content-Type": "application/json" } : {}, body: o.body ? JSON.stringify(o.body) : undefined });
