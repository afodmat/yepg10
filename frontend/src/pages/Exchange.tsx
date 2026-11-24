import { useState } from "react";
import { Icon } from "../components/Icon";
import { PageHero, Button } from "../components/ui";
import { Badge, Empty, ErrorNote, Loading, Pagination, statusTone } from "../components/common";
import { exchange, type Session } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";
import { useAction, useApi } from "../hooks/useApi";
import { fmtDay, fmtTime } from "../lib/format";
import { navigate } from "../lib/nav";

export function Exchange() {
  const { user } = useAuth();
  const [when, setWhen] = useState<"upcoming" | "completed">("upcoming");
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<number | null>(null);
  const list = useApi(() => exchange.list({ when, page, size: 6 }), [when, page]);
  const action = useAction();
  const current: Session | undefined = list.data?.items.find((s) => s.id === selected) ?? list.data?.items[0];

  const toggle = async (s: Session) => {
    if (!user) { navigate("login"); return; }
    await action.run(() => s.registered ? exchange.unregister(s.id) : exchange.register(s.id).then(() => undefined), s.registered ? "Registration cancelled." : "You're registered!");
    list.reload();
  };

  return <main><PageHero eyebrow="LEVEL ONE" title="Meet. Exchange. Understand." copy="Join facilitated conversations with peers from across East Africa and see the world through new perspectives." />
    <section className="mx-auto max-w-[1200px] px-5 py-14 lg:px-8">
      <div className="grid gap-6 lg:grid-cols-[1.3fr_.7fr]">
        <div>
          <div className="flex items-center justify-between"><div className="flex gap-2">{(["upcoming", "completed"] as const).map((w) => <button key={w} onClick={() => { setWhen(w); setPage(0); setSelected(null); }} className={`rounded-full px-4 py-2 text-sm font-bold capitalize ${when === w ? "bg-[#1a1a1a] text-white" : "bg-slate-100 text-slate-600"}`}>{w} sessions</button>)}</div>
            {list.data && <span className="rounded-full bg-[#efece6] px-3 py-1 text-xs font-bold text-[#8a6d2f]">{list.data.total} {when}</span>}</div>
          <div className="mt-4 space-y-2"><ErrorNote message={action.error || list.error} onRetry={list.reload} />{action.ok && <p role="status" className="rounded-xl bg-[#efece6] p-3 text-sm font-semibold text-[#8a6d2f]">{action.ok}</p>}</div>
          {list.loading && !list.data ? <Loading /> : list.data?.items.length === 0 ? <div className="mt-6"><Empty title={`No ${when} sessions`} hint="Check back soon — new sessions are added regularly." /></div> :
            <div className="mt-6 space-y-4">{list.data?.items.map((s, i) => <article key={s.id} onClick={() => setSelected(s.id)} className={`flex cursor-pointer flex-col gap-5 rounded-2xl border bg-white p-5 sm:flex-row sm:items-center ${current?.id === s.id ? "border-[#1a1a1a]" : "border-slate-200"}`}>
              <div className={`grid h-16 w-16 shrink-0 place-items-center rounded-2xl ${i === 0 ? "bg-[#efe5cf] text-[#8a6d2f]" : "bg-[#efece6] text-[#1a1a1a]"}`}><Icon name={i === 0 ? "spark" : "globe"} size={27} /></div>
              <div className="flex-1"><div className="flex flex-wrap items-center gap-2"><span className="text-[10px] font-extrabold uppercase tracking-wider text-[#8a6d2f]">{s.topic}</span><Badge tone={statusTone(s.phase === "UPCOMING" ? "SCHEDULED" : s.phase)}>{s.phase.toLowerCase()}</Badge></div>
                <h3 className="mt-1 text-lg font-extrabold text-[#1a1a1a]">{s.title}</h3>
                <p className="mt-1 text-sm text-slate-500">{fmtDay(s.startsAt)} · {fmtTime(s.startsAt)} · {s.durationMinutes} min{s.facilitatorName ? ` · ${s.facilitatorName}` : ""}</p>
                <p className="mt-1 text-xs text-slate-400">{s.participantCount} registered{s.countries.length > 0 ? ` · ${s.countries.join(", ")}` : ""}</p></div>
              {s.phase === "UPCOMING" && <Button variant={s.registered ? "ghost" : "secondary"} onClick={() => toggle(s)}>{s.registered ? "Registered ✓" : "Register"}</Button>}
            </article>)}</div>}
          {list.data && <Pagination page={list.data.page} totalPages={list.data.totalPages} total={list.data.total} onPage={setPage} />}
        </div>
        <aside className="h-fit rounded-[24px] bg-[#f6f5f2] p-6"><div className="grid h-12 w-12 place-items-center rounded-xl bg-white text-[#1a1a1a] shadow-sm"><Icon name="message" /></div>
          <h3 className="mt-5 text-xl font-extrabold text-[#1a1a1a]">Discussion prompts</h3>
          {current ? <>
            <p className="mt-1 text-sm font-semibold text-slate-500">{current.title}</p>
            {current.discussionPrompts.length ? <ul className="mt-4 space-y-3 text-sm leading-6 text-slate-600">{current.discussionPrompts.map((p) => <li key={p} className="rounded-xl bg-white p-3">{p}</li>)}</ul> : <p className="mt-3 text-sm text-slate-500">Prompts will be shared by the facilitator.</p>}
            {current.registered && current.phase === "UPCOMING" && current.meetingLink && <a href={current.meetingLink} target="_blank" rel="noopener noreferrer" className="mt-5 inline-flex min-h-10 w-full items-center justify-center rounded-xl bg-[#1a1a1a] px-4 text-sm font-bold text-white">Join the meeting</a>}
            {current.registered && current.resources.length > 0 && <div className="mt-5"><p className="text-xs font-extrabold uppercase tracking-wider text-slate-500">Resources</p><ul className="mt-2 space-y-1 text-sm text-slate-600">{current.resources.map((r) => <li key={r}>{r}</li>)}</ul></div>}
            {!current.registered && current.phase === "UPCOMING" && <p className="mt-5 text-xs text-slate-500">Register to see the meeting link and resources.</p>}
          </> : <p className="mt-3 text-sm text-slate-500">Select a session to see its prompts.</p>}
        </aside>
      </div>
    </section></main>;
}
