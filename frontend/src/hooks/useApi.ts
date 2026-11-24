import { useCallback, useEffect, useRef, useState, type DependencyList } from "react";
import { ApiError } from "../api/client";

export const errorMessage = (e: unknown) => e instanceof ApiError ? e.message : "Could not reach the server. Check your connection and try again.";

/** Loads data from the API; re-runs when deps change. `reload()` refreshes without clearing the screen. */
export function useApi<T>(load: () => Promise<T>, deps: DependencyList = []) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const seq = useRef(0);
  const run = useCallback(() => {
    const id = ++seq.current;
    setLoading(true);
    load().then((d) => { if (id === seq.current) { setData(d); setError(""); } })
      .catch((e) => { if (id === seq.current) setError(errorMessage(e)); })
      .finally(() => { if (id === seq.current) setLoading(false); });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => { run(); }, [run]);
  return { data, error, loading, reload: run, setData };
}

/** Runs a mutation with busy/error/success state for buttons and forms. */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const run = useCallback(async <R,>(fn: () => Promise<R>, success = ""): Promise<R | undefined> => {
    setBusy(true); setError(""); setOk("");
    try { const r = await fn(); setOk(success); return r; }
    catch (e) { setError(errorMessage(e)); return undefined; }
    finally { setBusy(false); }
  }, []);
  return { busy, error, ok, run, clear: () => { setError(""); setOk(""); } };
}

export function useDebounced<T>(value: T, ms = 350) {
  const [v, setV] = useState(value);
  useEffect(() => { const t = setTimeout(() => setV(value), ms); return () => clearTimeout(t); }, [value, ms]);
  return v;
}
