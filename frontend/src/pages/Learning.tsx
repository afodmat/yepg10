import { useState } from "react";
import { Icon } from "../components/Icon";
import { PageHero } from "../components/ui";
import { ResponsibleAI } from "../components/Chrome";
import { Empty, ErrorNote, Loading, Pagination } from "../components/common";
import { learning, type Activity } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";
import { useApi, useDebounced, useAction } from "../hooks/useApi";
import { CATEGORIES, categoryIcon, dateRange, tone } from "../lib/format";
import { navigate } from "../lib/nav";

export function ActivityCard({ activity: a, enrolled, compact = false, busy = false, onEnroll }: { activity: Activity; enrolled: boolean; compact?: boolean; busy?: boolean; onEnroll: () => void }) {
  const full = a.enrolledCount >= a.capacity;
  const closed = a.status !== "OPEN";
  return <article className="flex flex-col rounded-2xl border border-slate-200 bg-white p-6 transition hover:-translate-y-1 hover:shadow-lg">
    <div className="flex items-start justify-between"><span className={`grid h-11 w-11 place-items-center rounded-xl ${tone(a.category)}`}><Icon name={categoryIcon(a.category)} /></span><span className="rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider text-slate-600">{a.level}</span></div>
    <p className="mt-5 text-xs font-bold uppercase tracking-wider text-[#8a6d2f]">{a.category}</p>
    <h3 className="mt-2 text-lg font-extrabold leading-snug text-[#1a1a1a]">{a.title}</h3>
    <p className="mt-2 flex-1 text-sm leading-6 text-slate-500">{a.description}</p>
    <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-4 text-xs font-semibold text-slate-500"><span className="flex items-center gap-1.5"><Icon name="calendar" size={15} />{dateRange(a.startDate, a.endDate)}</span><span className="flex items-center gap-1.5"><Icon name="team" size={15} />{a.enrolledCount}</span></div>
    {!compact && <button onClick={onEnroll} disabled={enrolled || busy || closed || full} className={`mt-4 min-h-10 rounded-xl text-sm font-extrabold transition disabled:cursor-not-allowed ${enrolled ? "bg-[#efece6] text-[#8a6d2f]" : closed || full ? "bg-slate-100 text-slate-400" : "bg-[#1a1a1a] text-white hover:bg-[#1a1a1a]"}`}>{enrolled ? "Enrolled" : full ? "Full" : closed ? "Not open" : busy ? "Enrolling…" : "Enroll now"}</button>}
  </article>;
}

export function Learning() {
  const { user } = useAuth();
  const [query, setQuery] = useState("");
  const [level, setLevel] = useState("");
  const [category, setCategory] = useState("");
  const [page, setPage] = useState(0);
  const q = useDebounced(query);
  const list = useApi(() => learning.list({ q, level, category, page, size: 9 }), [q, level, category, page]);
  const mine = useApi(async () => user ? learning.mine() : [], [user?.id]);
  const action = useAction();
  const enrolledIds = new Set((mine.data ?? []).map((e) => e.activityId));

  const enroll = async (id: number) => {
    if (!user) { navigate("login"); return; }
    if (await action.run(() => learning.enroll(id), "Enrolled! Find it under My learning.")) { mine.reload(); list.reload(); }
  };

  return <main>
    <PageHero eyebrow="LEARNING HUB" title="Build skills that open doors" copy="Practical, accessible learning designed with and for young people in East Africa." />
    <section className="mx-auto max-w-[1200px] px-5 py-12 lg:px-8">
      <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-3 sm:flex-row">
        <label className="relative flex-1"><span className="sr-only">Search activities</span><Icon name="search" className="absolute left-4 top-3.5 text-slate-400" size={18} /><input value={query} onChange={(e) => { setQuery(e.target.value); setPage(0); }} className="h-11 w-full rounded-xl bg-slate-50 pl-11 pr-4 text-sm outline-none focus:ring-2 focus:ring-[#c2a15a]" placeholder="Search learning activities..." /></label>
        <select aria-label="Category" value={category} onChange={(e) => { setCategory(e.target.value); setPage(0); }} className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 outline-none"><option value="">All categories</option>{CATEGORIES.map((c) => <option key={c}>{c}</option>)}</select>
        <select aria-label="Level" value={level} onChange={(e) => { setLevel(e.target.value); setPage(0); }} className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 outline-none"><option value="">All levels</option><option>Level 1</option><option>Level 2</option></select>
      </div>
      <div className="mt-8 flex items-center justify-between"><p className="text-sm font-semibold text-slate-500"><strong className="text-slate-900">{list.data?.total ?? 0}</strong> activities available</p><span className="text-xs text-slate-400">Low-bandwidth friendly</span></div>
      {action.ok && <p role="status" className="mt-4 rounded-xl bg-[#efece6] p-3 text-sm font-semibold text-[#8a6d2f]">{action.ok}</p>}
      <div className="mt-4"><ErrorNote message={action.error || list.error} onRetry={list.reload} /></div>
      {list.loading && !list.data ? <Loading /> : list.data && list.data.items.length === 0 ? <div className="mt-5"><Empty title="No activities match your search" hint="Try a different word or clear the filters." /></div> :
        <div className="mt-5 grid gap-5 md:grid-cols-2 lg:grid-cols-3">{list.data?.items.map((a) => <ActivityCard key={a.id} activity={a} enrolled={enrolledIds.has(a.id)} busy={action.busy} onEnroll={() => enroll(a.id)} />)}</div>}
      {list.data && <Pagination page={list.data.page} totalPages={list.data.totalPages} total={list.data.total} onPage={setPage} />}
    </section>
    <ResponsibleAI />
  </main>;
}
