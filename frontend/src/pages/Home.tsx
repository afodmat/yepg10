import { FormEvent, ReactNode, useEffect, useMemo, useState, type CSSProperties } from "react";
import { ActivityCard } from "./Learning";
import type { Page, IconName } from "../types";
import { Icon } from "../components/Icon";
import { navigate } from "../lib/nav";
import { Logo, Button, Field, PageHero } from "../components/ui";
import { learning, dashboard } from "../api/endpoints";
import { useApi } from "../hooks/useApi";
import { Loading, Empty } from "../components/common";
import { PublicHeader } from "../components/Chrome";

export function Home() {
  const stats = useApi(() => dashboard.publicStats());
  const popular = useApi(() => learning.list({ size: 3 }));
  const statRows: [string, string][] = stats.data ? [[String(stats.data.participants), "Young people"], [String(stats.data.countries), "Countries represented"], [String(stats.data.activities), "Learning activities"], [String(stats.data.projects), "Impact projects"]] : [];
  return <>
    <section className="relative isolate overflow-hidden bg-[#1a1a1a] text-white" style={{ backgroundImage: "linear-gradient(180deg,rgba(0,0,0,.25),rgba(0,0,0,.65)), url(/hero.jpg)", backgroundSize: "cover", backgroundPosition: "center" }}>
      <div className="mx-auto flex min-h-[78vh] max-w-[1320px] flex-col justify-end px-5 pb-36 pt-24 lg:px-10">
        <p className="eyebrow !text-[#dcc690]">Open to East African youth aged 18–35</p>
        <h1 className="mt-5 max-w-4xl text-5xl leading-[1.05] sm:text-6xl lg:text-[84px]">Learn digital skills. Create real impact.</h1>
        <p className="mt-6 max-w-xl text-lg font-light leading-8 text-white/80">Connect with young changemakers, learn future-ready skills, and turn your ideas into community solutions with expert mentorship.</p>
        <div className="mt-10 flex flex-col gap-3 sm:flex-row">
          <Button onClick={() => navigate("register")} className="!border-white !bg-white !text-[#1a1a1a] hover:!border-[#c2a15a] hover:!bg-[#c2a15a]">Join YEP, it’s free</Button>
          <Button onClick={() => navigate("learning")} variant="ghost" className="!border-white/60 !text-white hover:!border-white hover:!bg-white hover:!text-[#1a1a1a]">Explore activities</Button>
        </div>
      </div>
    </section>

    <nav aria-label="Quick links" className="relative z-10 mx-auto -mt-16 max-w-[1100px] px-5 lg:px-8">
      <div className="grid grid-cols-2 divide-x divide-slate-200 border border-slate-200 bg-white shadow-[0_20px_50px_rgba(0,0,0,.12)] md:grid-cols-4">
        {([["learning","Learn","book"],["exchange","Exchange","globe"],["projects","Projects","team"],["mentorship","Mentorship","leaf"]] as [Page,string,IconName][]).map(([pg,label,ic]) => <button key={pg} onClick={() => navigate(pg)} className="group flex flex-col items-center gap-3 px-4 py-7 text-[11px] uppercase tracking-[.24em] text-[#1a1a1a] transition-colors hover:bg-[#f6f5f2]"><Icon name={ic} size={26} className="text-[#8a6d2f]"/>{label}</button>)}
      </div>
    </nav>

    {statRows.length > 0 && <section className="border-b border-slate-200 bg-white">
      <div className="mx-auto grid max-w-[1100px] grid-cols-2 divide-x divide-slate-200 px-5 py-7 md:grid-cols-4">
        {statRows.map(([number,label]) => <div className="px-4 py-2 text-center" key={label}><p className="text-2xl font-extrabold text-[#1a1a1a]">{number}</p><p className="mt-1 text-xs font-semibold text-slate-500">{label}</p></div>)}
      </div>
    </section>}

    <section className="mx-auto max-w-[1200px] px-5 py-20 lg:px-8">
      <div className="mx-auto max-w-2xl text-center">
        <p className="eyebrow">YOUR YEP JOURNEY</p>
        <h2 className="section-title">From learner to changemaker</h2>
        <p className="section-copy">Two connected levels take you from building essential skills to creating solutions for real community challenges.</p>
      </div>
      <div className="mt-12 grid gap-6 lg:grid-cols-2">
        <article className="group rounded-none border border-slate-200 bg-white p-7 transition hover:-translate-y-1 hover:shadow-xl sm:p-9">
          <div className="flex items-start justify-between"><span className="grid h-14 w-14 place-items-center rounded-2xl bg-[#efece6] text-[#1a1a1a]"><Icon name="globe" size={28} style={{ position: "relative" }}/></span><span className="rounded-full bg-[#efece6] px-3 py-1 text-xs font-extrabold uppercase tracking-wider text-[#1a1a1a]">Level one</span></div>
          <h3 className="relative mt-7 text-2xl font-extrabold text-[#1a1a1a]">Virtual Exchange</h3>
          <p className="mt-3 leading-7 text-slate-600">Learn with diverse peers in lively online sessions designed to strengthen your digital confidence and global perspective.</p>
          <div className="mt-6 grid gap-3 sm:grid-cols-2">{["Digital & AI literacy","Critical thinking","Media literacy","Intercultural skills"].map(x=><span key={x} className="flex items-center gap-2 text-sm font-semibold text-slate-700"><span className="grid h-5 w-5 place-items-center rounded-full bg-[#efece6] text-[#8a6d2f]"><Icon name="check" size={13}/></span>{x}</span>)}</div>
          <button onClick={() => navigate("exchange")} className="mt-7 inline-flex items-center gap-2 text-sm font-extrabold text-[#1a1a1a]">Explore virtual exchange <Icon name="arrow" size={17}/></button>
        </article>
        <article className="group rounded-none border border-slate-200 bg-[#f6f5f2] p-7 transition hover:-translate-y-1 hover:shadow-xl sm:p-9">
          <div className="flex items-start justify-between"><span className="grid h-14 w-14 place-items-center rounded-2xl bg-[#efece6] text-[#8a6d2f]"><Icon name="leaf" size={28}/></span><span className="rounded-full bg-[#efece6] px-3 py-1 text-xs font-extrabold uppercase tracking-wider text-[#8a6d2f]">Level two</span></div>
          <h3 className="mt-7 text-2xl font-extrabold text-[#1a1a1a]">Impact Projects</h3>
          <p className="mt-3 leading-7 text-slate-600">Join a multidisciplinary team, work with a mentor, and develop a practical solution that matters to your community.</p>
          <div className="mt-6 grid gap-3 sm:grid-cols-2">{["Team collaboration","Expert mentorship","Project funding skills","Impact recognition"].map(x=><span key={x} className="flex items-center gap-2 text-sm font-semibold text-slate-700"><span className="grid h-5 w-5 place-items-center rounded-full bg-[#efece6] text-[#8a6d2f]"><Icon name="check" size={13}/></span>{x}</span>)}</div>
          <button onClick={() => navigate("projects")} className="mt-7 inline-flex items-center gap-2 text-sm font-extrabold text-[#8a6d2f]">Discover impact projects <Icon name="arrow" size={17}/></button>
        </article>
      </div>
    </section>

    <section className="bg-[#f6f5f2] py-20">
      <div className="mx-auto max-w-[1200px] px-5 lg:px-8">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end"><div><p className="eyebrow">POPULAR LEARNING</p><h2 className="section-title">Skills for your future</h2></div><button onClick={() => navigate("learning")} className="flex items-center gap-2 text-sm font-extrabold text-[#1a1a1a]">View all activities <Icon name="arrow" size={17}/></button></div>
        <div className="mt-10 grid gap-5 md:grid-cols-3">
          {popular.data?.items.map(a=><ActivityCard key={a.id} activity={a} enrolled={false} compact onEnroll={()=>navigate("register")}/>)}
          {!popular.data && !popular.error && <Loading/>}
          {popular.data?.items.length === 0 && <Empty title="New activities are coming soon"/>}
        </div>
      </div>
    </section>

    <section className="bg-[#1a1a1a] py-16 text-white">
      <div className="mx-auto flex max-w-[1000px] flex-col items-center justify-between gap-8 px-5 text-center md:flex-row md:text-left">
        <div><p className="text-sm font-bold uppercase tracking-[.17em] text-[#dcc690]">YOUR IDEAS MATTER</p><h2 className="mt-3 text-3xl font-extrabold tracking-tight">Ready to build a better future?</h2><p className="mt-2 text-white/70">Join a growing community of East African changemakers.</p></div>
        <Button onClick={()=>navigate("register")} className="shrink-0 px-7">Create your free account <Icon name="arrow" size={17}/></Button>
      </div>
    </section>
  </>;
}

