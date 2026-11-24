import { ReactNode, useEffect, useState } from "react";
import { Icon } from "../components/Icon";
import { Logo } from "../components/ui";
import { Avatar } from "../components/common";
import { navigate } from "../lib/nav";
import { messaging, notices, fileUrl } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";
import type { IconName, Page } from "../types";
import type { Role } from "../api/auth";

type NavItem = { page: Page; label: string; icon: IconName };
const common: NavItem[] = [{ page: "messages", label: "Messages", icon: "mail" }, { page: "notifications", label: "Notifications", icon: "bell" }, { page: "profile", label: "My profile", icon: "user" }];
export const navFor = (role: Role): NavItem[] => ({
  PARTICIPANT: [{ page: "dashboard", label: "Overview", icon: "grid" }, { page: "mylearning", label: "My learning", icon: "book" }, { page: "exchange", label: "Virtual exchange", icon: "globe" }, { page: "team", label: "My team & project", icon: "team" }, { page: "mentorship", label: "Mentorship", icon: "leaf" }, { page: "achievements", label: "Achievements", icon: "trophy" }, ...common],
  MENTOR: [{ page: "dashboard", label: "My mentees", icon: "team" }, ...common],
  FACILITATOR: [{ page: "dashboard", label: "Manage", icon: "grid" }, ...common],
  ADMIN: [{ page: "dashboard", label: "Administration", icon: "shield" }, ...common],
} as Record<Role, NavItem[]>)[role];

const roleLabel: Record<Role, string> = { PARTICIPANT: "Youth Participant", MENTOR: "Mentor", FACILITATOR: "Facilitator", ADMIN: "Administrator" };
const countryCodes: Record<string, string> = {
  uganda: "UG",
  kenya: "KE",
  tanzania: "TZ",
  "united republic of tanzania": "TZ",
  rwanda: "RW",
  burundi: "BI",
  "south sudan": "SS",
};
const countryFlag = (country: string) => countryCodes[country.trim().toLowerCase()];

export function DashboardLayout({ page, children, onLogout, unreadMessages, unreadNotices }: { page: Page; children: ReactNode; onLogout: () => void; unreadMessages: number; unreadNotices: number }) {
  const { user } = useAuth();
  const [mobile, setMobile] = useState(false);
  if (!user) return null;
  const fullName = `${user.firstName} ${user.lastName}`;
  const items = navFor(user.role);
  const badge = (n: number) => n > 0 && <span className="ml-auto grid h-5 min-w-5 place-items-center rounded-full bg-[#c2a15a] px-1 text-[10px] text-[#1a1a1a]">{n > 99 ? "99+" : n}</span>;
  return <div className="min-h-screen bg-[#f6f5f2] lg:flex">
    <aside className={`fixed inset-y-0 left-0 z-50 flex w-72 flex-col overflow-y-auto bg-[#1a1a1a] p-5 text-white transition-transform lg:sticky lg:top-0 lg:h-screen lg:translate-x-0 ${mobile ? "translate-x-0" : "-translate-x-full"}`}>
      <div className="flex shrink-0 items-center justify-between"><Logo light /><button onClick={() => setMobile(false)} aria-label="Close menu" className="p-2 lg:hidden"><Icon name="close" /></button></div>
      <button onClick={() => { navigate("profile"); setMobile(false); }} className="mt-9 flex w-full shrink-0 items-center gap-3 rounded-2xl bg-white/10 p-4 text-left">
        {user.photoFileId ? <img src={fileUrl(user.photoFileId)} alt="" loading="lazy" className="h-11 w-11 rounded-full object-cover" /> : <Avatar name={fullName} size="h-11 w-11" className="bg-[#c2a15a] text-[#1a1a1a]" />}
        <div className="min-w-0"><p className="truncate font-bold">{fullName}</p><p className="text-xs text-white/55">{roleLabel[user.role]}</p>{user.country && <p className="flex items-center gap-1.5 truncate text-xs text-white/70">{countryFlag(user.country) ? <img src={`/country-flags/${countryFlag(user.country)}.svg`} alt={`${user.country} flag`} className="h-3.5 w-5 shrink-0 rounded-[2px] object-cover" /> : <span aria-hidden="true">🌍</span>}<span>From {user.country}</span></p>}</div></button>
      <nav className="mt-7 space-y-1" aria-label="Dashboard">{items.map((n) => <button key={n.page} onClick={() => { navigate(n.page); setMobile(false); }} aria-current={page === n.page ? "page" : undefined} className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition ${page === n.page ? "bg-white text-[#1a1a1a]" : "text-white/65 hover:bg-white/10 hover:text-white"}`}><Icon name={n.icon} size={18} />{n.label}{n.page === "messages" ? badge(unreadMessages) : n.page === "notifications" ? badge(unreadNotices) : null}</button>)}</nav>
      <button onClick={onLogout} className="mt-auto w-full shrink-0 rounded-xl border border-white/15 py-2.5 text-sm font-bold text-white/60 hover:text-white">Log out</button>
    </aside>
    {mobile && <button aria-label="Close menu" className="fixed inset-0 z-40 bg-black/40 lg:hidden" onClick={() => setMobile(false)} />}
    <div className="min-w-0 flex-1"><header className="flex h-[72px] items-center justify-between border-b border-slate-200 bg-white px-5 lg:px-9"><button onClick={() => setMobile(true)} aria-label="Open menu" className="p-2 text-[#1a1a1a] lg:hidden"><Icon name="menu" /></button>
      <p className="hidden text-sm font-semibold text-slate-500 sm:block">{new Date().toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long", year: "numeric" })}</p>
      <div className="ml-auto flex items-center gap-3"><button onClick={() => navigate("notifications")} aria-label={`Notifications${unreadNotices ? `, ${unreadNotices} unread` : ""}`} className="relative grid h-10 w-10 place-items-center rounded-full bg-slate-100 text-[#1a1a1a]"><Icon name="bell" size={18} />{unreadNotices > 0 && <span className="absolute -right-0.5 -top-0.5 h-3 w-3 rounded-full border-2 border-white bg-[#c2a15a]" />}</button>
        <button onClick={() => navigate("messages")} aria-label="Messages" className="relative grid h-10 w-10 place-items-center rounded-full bg-slate-100 text-[#1a1a1a]"><Icon name="mail" size={18} />{unreadMessages > 0 && <span className="absolute -right-0.5 -top-0.5 h-3 w-3 rounded-full border-2 border-white bg-[#c2a15a]" />}</button></div></header>
      {children}</div>
  </div>;
}

/** Polls unread counts every minute (and when the tab becomes visible) — two tiny requests, no push needed. */
export function useUnread(enabled: boolean) {
  const [counts, setCounts] = useState({ messages: 0, notices: 0 });
  const refresh = () => { if (!enabled) return; Promise.all([messaging.unread(), notices.unread()]).then(([m, n]) => setCounts({ messages: m.count, notices: n.count })).catch(() => undefined); };
  useEffect(() => {
    refresh();
    if (!enabled) return;
    const t = setInterval(() => { if (!document.hidden) refresh(); }, 60000);
    const v = () => { if (!document.hidden) refresh(); };
    document.addEventListener("visibilitychange", v);
    return () => { clearInterval(t); document.removeEventListener("visibilitychange", v); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled]);
  return { ...counts, refresh };
}
