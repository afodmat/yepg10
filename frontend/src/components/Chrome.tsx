import { FormEvent, ReactNode, useEffect, useMemo, useState, type CSSProperties } from "react";
import type { Page, IconName } from "../types";
import { Icon } from "../components/Icon";
import { navigate } from "../lib/nav";
import { Logo, Button, Field, PageHero } from "../components/ui";

export function PublicHeader({ loggedIn }: { loggedIn: boolean }) {
  const [open, setOpen] = useState(false);
  const links: [Page, string][] = [["learning", "Learn"], ["exchange", "Exchange"], ["projects", "Projects"], ["mentorship", "Mentorship"]];
  const navCls = "text-[11px] font-medium uppercase tracking-[.24em] text-[#1a1a1a] transition-colors hover:text-[#8a6d2f]";
  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex h-20 max-w-[1320px] items-center justify-between px-5 lg:px-10">
        <Logo />
        <nav className="hidden items-center gap-10 lg:flex" aria-label="Main">
          {links.map(([page, label]) => <button key={page} onClick={() => navigate(page)} className={navCls}>{label}</button>)}
        </nav>
        <div className="hidden items-center gap-6 lg:flex">
          {loggedIn ? <Button onClick={() => navigate("dashboard")} variant="secondary" className="min-h-10 px-5">My dashboard</Button> : <>
            <button onClick={() => navigate("login")} className={navCls}>Log in</button>
            <Button onClick={() => navigate("register")} className="min-h-10 px-5">Join</Button>
          </>}
        </div>
        <button onClick={() => setOpen(!open)} className="p-2 text-[#1a1a1a] lg:hidden" aria-label={open ? "Close menu" : "Open menu"} aria-expanded={open}><Icon name={open ? "close" : "menu"} /></button>
      </div>
      {open && <div className="border-t border-slate-200 bg-white px-5 py-6 lg:hidden">
        {links.map(([page, label]) => <button key={page} onClick={() => { navigate(page); setOpen(false); }} className="block w-full border-b border-slate-100 py-4 text-left font-[Gilda_Display] text-2xl">{label}</button>)}
        <Button onClick={() => navigate(loggedIn ? "dashboard" : "register")} className="mt-6 w-full">{loggedIn ? "My dashboard" : "Join YEP"}</Button>
      </div>}
    </header>
  );
}

export function ResponsibleAI() {
  return <section className="border-y border-slate-200 bg-[#f6f5f2] py-20"><div className="mx-auto grid max-w-[1100px] gap-8 px-5 md:grid-cols-[auto_1fr] md:items-start md:gap-14 lg:px-8"><span className="grid h-14 w-14 place-items-center border border-[#c2a15a] text-[#8a6d2f]"><Icon name="spark" size={26}/></span><div><p className="eyebrow">Responsible AI</p><h2 className="mt-3 text-3xl leading-tight sm:text-4xl">Use AI to strengthen your thinking, not replace it.</h2><p className="mt-4 max-w-2xl font-light leading-7 text-slate-600">Always verify AI-generated information, protect personal data, recognise misinformation and deepfakes, and never share sensitive information with AI systems.</p></div></div></section>;
}

export function Footer() {
  const col = "block py-1 text-left text-sm font-light text-white/70 transition-colors hover:text-[#dcc690]";
  return <footer className="bg-[#1a1a1a] px-5 pb-10 pt-16 text-white/70 lg:px-10"><div className="mx-auto grid max-w-[1320px] gap-12 md:grid-cols-[1.4fr_1fr_1fr]"><div><Logo light/><p className="mt-6 max-w-sm text-sm font-light leading-7">A youth digital empowerment and virtual exchange platform connecting East African changemakers.</p></div><div><p className="text-[11px] uppercase tracking-[.24em] text-[#dcc690]">Explore</p><div className="mt-4"><button className={col} onClick={()=>navigate("learning")}>Learning</button><button className={col} onClick={()=>navigate("projects")}>Impact projects</button><button className={col} onClick={()=>navigate("mentorship")}>Mentorship</button></div></div><div><p className="text-[11px] uppercase tracking-[.24em] text-[#dcc690]">Account</p><div className="mt-4"><button className={col} onClick={()=>navigate("login")}>Log in</button><button className={col} onClick={()=>navigate("register")}>Join YEP</button></div></div></div><div className="mx-auto mt-14 max-w-[1320px] border-t border-white/15 pt-6 text-xs tracking-wider">© 2026 Youth Empowerment Project. Learning without borders.</div></footer>;
}
