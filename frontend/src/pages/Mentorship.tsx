import { useState } from "react";
import { Icon } from "../components/Icon";
import { PageHero, Button } from "../components/ui";
import { Avatar, Empty, ErrorNote, Labeled, Loading, Modal, Pagination, areaCls } from "../components/common";
import { mentorship, type Mentor } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";
import { useAction, useApi } from "../hooks/useApi";
import { navigate } from "../lib/nav";

export function Mentorship() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [target, setTarget] = useState<Mentor | null>(null);
  const [message, setMessage] = useState("");
  const mentors = useApi(() => mentorship.mentors({ page }), [page]);
  const action = useAction();

  const send = async () => {
    if (!target) return;
    if (await action.run(() => mentorship.request(target.userId, message), "")) { setTarget(null); setMessage(""); navigate("mentees"); }
  };
  const ask = (m: Mentor) => user ? setTarget(m) : navigate("login");

  return <main><PageHero eyebrow="GUIDANCE THAT GROWS WITH YOU" title="Learn from people who care" copy="Our mentors help teams move from uncertainty to clear, practical next steps." />
    <section className="mx-auto grid max-w-[1100px] gap-12 px-5 py-16 lg:grid-cols-2 lg:items-center">
      <div><p className="eyebrow">HOW MENTORSHIP WORKS</p><h2 className="section-title">Support beyond the classroom</h2><div className="mt-8 space-y-6">{[["01", "Get matched", "Request a mentor whose experience fits your goals, or be matched by the YEP team."], ["02", "Meet consistently", "Short, focused check-ins, with notes and feedback captured on your project."], ["03", "Build with confidence", "Mentors challenge assumptions, open networks, and celebrate progress."]].map((x) => <div className="flex gap-4" key={x[0]}><span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-[#efece6] text-sm font-extrabold text-[#8a6d2f]">{x[0]}</span><div><h3 className="font-extrabold text-[#1a1a1a]">{x[1]}</h3><p className="mt-1 text-sm leading-6 text-slate-500">{x[2]}</p></div></div>)}</div></div>
      <div className="rounded-none bg-[#1a1a1a] p-8 text-white"><Icon name="team" size={38} className="text-[#dcc690]" /><p className="mt-6 text-2xl font-bold leading-9">The best solutions come when young people are trusted to lead—and supported to keep going.</p><p className="mt-6 border-t border-white/15 pt-5 text-sm text-white/55">The YEP mentorship approach</p></div>
    </section>
    <section className="bg-[#f6f5f2] py-14"><div className="mx-auto max-w-[1200px] px-5 lg:px-8">
      <p className="eyebrow">OUR MENTORS</p><h2 className="section-title">Meet the people who can help</h2>
      <div className="mt-6"><ErrorNote message={mentors.error} onRetry={mentors.reload} /></div>
      {mentors.loading && !mentors.data ? <Loading /> : mentors.data?.items.length === 0 ? <Empty title="Mentors are joining soon" /> :
        <div className="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-3">{mentors.data?.items.map((m) => <article key={m.userId} className="flex flex-col rounded-2xl border border-slate-200 bg-white p-6">
          <div className="flex items-center gap-3"><Avatar name={m.name} size="h-12 w-12" /><div><p className="font-extrabold text-[#1a1a1a]">{m.name}</p><p className="text-xs text-slate-500">{m.country}</p></div></div>
          <p className="mt-4 text-xs font-bold uppercase tracking-wider text-[#8a6d2f]">Expertise</p><p className="mt-1 text-sm text-slate-600">{m.expertise}</p>
          {m.bio && <p className="mt-3 flex-1 text-sm leading-6 text-slate-500">{m.bio}</p>}
          <p className="mt-3 text-xs text-slate-400">{m.availability}</p>
          {user?.role === "PARTICIPANT" || !user ? <Button variant="secondary" className="mt-4" onClick={() => ask(m)}>{m.accepting ? "Request mentorship" : "Currently full"}</Button> : null}
        </article>)}</div>}
      {mentors.data && <Pagination page={mentors.data.page} totalPages={mentors.data.totalPages} total={mentors.data.total} onPage={setPage} />}
    </div></section>
    {target && <Modal title={`Ask ${target.name} to mentor you`} onClose={() => setTarget(null)}>
      <Labeled label="Tell them about your goals" hint="A sentence or two helps your mentor decide."><textarea className={areaCls} maxLength={1000} value={message} onChange={(e) => setMessage(e.target.value)} /></Labeled>
      <div className="mt-3"><ErrorNote message={action.error} /></div>
      <div className="mt-5 flex justify-end"><Button onClick={send}>{action.busy ? "Sending…" : "Send request"}</Button></div></Modal>}
  </main>;
}
