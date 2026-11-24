import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, http, qs } from "../api/client";

const res = (status: number, body: unknown = {}) => Promise.resolve(new Response(status === 204 ? null : JSON.stringify(body), { status }));
afterEach(() => { vi.unstubAllGlobals(); document.cookie = "XSRF-TOKEN=; max-age=0"; });

describe("api client", () => {
  it("builds query strings without empty values", () => {
    expect(qs({ q: "ai", page: 0, level: "", x: undefined })).toBe("?q=ai&page=0");
    expect(qs({})).toBe("");
  });

  it("echoes the CSRF cookie in a header on mutating requests", async () => {
    document.cookie = "XSRF-TOKEN=abc123";
    const f = vi.fn(() => res(204)); vi.stubGlobal("fetch", f);
    await http.post("/teams/1/join");
    const init = (f.mock.calls[0] as unknown as [string, RequestInit])[1];
    expect((init.headers as Record<string, string>)["X-XSRF-TOKEN"]).toBe("abc123");
  });

  it("fetches a CSRF cookie before a mutation when one is not present", async () => {
    const f = vi.fn((url: string | URL | Request, _init?: RequestInit) => {
      if (String(url).endsWith("/auth/csrf")) {
        document.cookie = "XSRF-TOKEN=bootstrapped";
        return res(200);
      }
      return res(204);
    });
    vi.stubGlobal("fetch", f);
    await http.post("/teams/1/join");
    expect(f.mock.calls.map(([url]) => String(url))).toEqual(["/api/auth/csrf", "/api/teams/1/join"]);
    expect(f.mock.calls[1]?.[1]?.headers).toMatchObject({ "X-XSRF-TOKEN": "bootstrapped" });
  });

  it("turns error responses into ApiError with field errors", async () => {
    vi.stubGlobal("fetch", vi.fn(() => res(400, { message: "Invalid request", fieldErrors: { age: "YEP is for ages 18–35" } })));
    await expect(http.post("/auth/register", {})).rejects.toMatchObject({ status: 400, fieldErrors: { age: "YEP is for ages 18–35" } });
    await expect(http.post("/auth/register", {})).rejects.toBeInstanceOf(ApiError);
  });

  it("refreshes the session once on 401 and retries the request", async () => {
    const seq = [res(401, { message: "expired" }), res(200, {}), res(200, {}), res(200, { ok: true })];
    const f = vi.fn((url: string | URL | Request, _init?: RequestInit) => {
      if (String(url).endsWith("/auth/csrf")) document.cookie = "XSRF-TOKEN=refreshed";
      return seq.shift()!;
    }); vi.stubGlobal("fetch", f);
    await expect(http.get("/enrollments/mine")).resolves.toEqual({ ok: true });
    expect(f.mock.calls.map((c) => String(c[0]))).toEqual(["/api/enrollments/mine", "/api/auth/csrf", "/api/auth/refresh", "/api/enrollments/mine"]);
    expect(f.mock.calls[2]?.[1]?.headers).toMatchObject({ "X-XSRF-TOKEN": "refreshed" });
  });
});
