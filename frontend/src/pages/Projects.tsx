import { useState } from "react";
import { Icon } from "../components/Icon";
import { PageHero } from "../components/ui";
import { Empty, ErrorNote, Loading, Pagination, ProgressBar } from "../components/common";
import { projects } from "../api/endpoints";
import { useApi } from "../hooks/useApi";
import { PROJECT_CATEGORIES } from "../lib/format";

const bar = (c: string) => ({ Agriculture: "bg-[#c2a15a]", Employment: "bg-[#c2a15a]", Health: "bg-[#8a8a85]", Education: "bg-[#9c8ce7]", "Climate Change": "bg-[#c2a15a]", "Digital Inclusion": "bg-[#e87979]" } as Record<string, string>)[c] ?? "bg-[#8a8a85]";

export function Projects() {
  const [filter, setFilter] = useState("");
  const [page, setPage] = useState(0);
  const list = useApi(() => projects.list({ category: filter, page, size: 9 }), [filter, page]);
  return <main><PageHero eyebrow="LEVEL TWO" title="Ideas into action" copy="Explore youth-led solutions tackling the issues that matter most across our communities." />
    <section className="mx-auto max-w-[1200px] px-5 py-14 lg:px-8">
      <div className="flex flex-wrap gap-2">{["", ...PROJECT_CATEGORIES].map((x) => <button onClick={() => { setFilter(x); setPage(0); }} className={`rounded-full px-4 py-2 text-sm font-bold ${filter === x ? "bg-[#1a1a1a] text-white" : "bg-slate-100 text-slate-600"}`} key={x}>{x || "All"}</button>)}</div>
      <div className="mt-6"><ErrorNote message={list.error} onRetry={list.reload} /></div>
      {list.loading && !list.data ? <Loading /> : list.data?.items.length === 0 ? <Empty title="No projects here yet" hint="Teams publish their Impact Projects as they start building." /> :
        <div className="mt-8 grid gap-6 md:grid-cols-3">{list.data?.items.map((p) => <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white" key={p.id}><div className={`h-2 ${bar(p.category)}`} />
          <div className="p-6"><div className="flex items-center justify-between"><span className="text-xs font-extrabold uppercase tracking-wider text-[#8a6d2f]">{p.category}</span><span className="text-xs font-bold text-slate-400">{p.location}</span></div>
            <h3 className="mt-3 text-xl font-extrabold text-[#1a1a1a]">{p.name}</h3><p className="mt-2 text-sm leading-6 text-slate-500">{p.description}</p>
            <div className="mt-5 flex items-center justify-between text-xs font-bold"><span className="flex items-center gap-1.5 text-slate-500"><Icon name="team" size={15} />{p.teamName}</span><span>{p.progressPct}%</span></div>
            <div className="mt-2"><ProgressBar value={p.progressPct} tone={bar(p.category)} /></div></div></article>)}</div>}
      {list.data && <Pagination page={list.data.page} totalPages={list.data.totalPages} total={list.data.total} onPage={setPage} />}
    </section></main>;
}
