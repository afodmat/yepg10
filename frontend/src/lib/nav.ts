import { useEffect, useState } from "react";

/** Hash routing: "#learning", "#verify/ABC123", "#activate?token=xyz". */
export function navigate(target: string) {
  if (location.hash.slice(1) === target) window.dispatchEvent(new HashChangeEvent("hashchange"));
  else location.hash = target;
  window.scrollTo({ top: 0 });
}

export interface Route { page: string; param?: string; query: URLSearchParams }
function parse(): Route {
  const [path, q = ""] = location.hash.slice(1).split("?");
  const [page, param] = path.split("/");
  return { page: page || "home", param: param || undefined, query: new URLSearchParams(q) };
}
export function useRoute(): Route {
  const [route, setRoute] = useState<Route>(parse);
  useEffect(() => {
    const on = () => setRoute(parse());
    addEventListener("hashchange", on);
    return () => removeEventListener("hashchange", on);
  }, []);
  return route;
}
