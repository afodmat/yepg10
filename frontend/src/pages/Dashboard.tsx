import { Icon } from "../components/Icon";
import { Button } from "../components/ui";
import { Avatar, Card, Empty, ErrorNote, Loading, PageTitle, ProgressBar } from "../components/common";
import { dashboard } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";
import { ago, categoryIcon, fmtDay, fmtTime, tone } from "../lib/format";
import { navigate } from "../lib/nav";

export function Dashboard() {
  const { user } = useAuth();
  const d = useApi(() => dashboard.get());
  if (d.loading && !d.data) return <main className="p-5 lg:p-9"><Loading /></main>;
  if (!d.data) return <main className="p-5 lg:p-9"><ErrorNote message={d.error} onRetry={d.reload} /></main>;
  const x = d.data;
  const active = x.learning.filter((e) => e.status === "ENROLLED");
  const avg = x.learning.length ? Math.round(x.learning.reduce((n, e) => n + e.progressPct, 0) / x.learning.length) : 0;
  const stats = [["Activities enrolled", String(x.learning.length), "book", "bg-[#efece6] text-[#1a1a1a]"], ["Learning progress", `${avg}%`, "spark", "bg-[#efece6] text-[#8a6d2f]"], ["Project progress", x.team?.project ? `${x.team.project.progressPct}%` : "—", "leaf", "bg-[#efe5cf] text-[#8a6d2f]"], ["Achievements", `${x.achievementsEarned}/${x.achievementsTotal}`, "trophy", "bg-[#eeebff] text-[#6652bf]"]] as const;
  const next = x.upcomingSessions[0];

  return <main className="p-5 lg:p-9">
    <PageTitle eyebrow={`Welcome back, ${user?.firstName}`} title="Keep your momentum going" sub={active.length ? `You have ${active.length} activit${active.length === 1 ? "y" : "ies"} in progress.` : "Start your first learning activity today."} action={<Button onClick={() => navigate("learning")}>Explore activities <Icon name="arrow" size={16} /></Button>} />
    {x.pendingInvitations > 0 && <button onClick={() => navigate("team")} className="mt-5 w-full rounded-xl border border-[#efe5cf] bg-[#efe5cf] p-4 text-left text-sm font-bold text-[#8a6d2f]">You have {x.pendingInvitations} team invitation{x.pendingInvitations > 1 ? "s" : ""} waiting →</button>}
    <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{stats.map(([label, value, icon, color]) => <div key={label} className="rounded-2xl border border-slate-200 bg-white p-5"><div className="flex items-start justify-between"><span className={`grid h-10 w-10 place-items-center rounded-xl ${color}`}><Icon name={icon} size={19} /></span><span className="text-2xl font-extrabold text-[#1a1a1a]">{value}</span></div><p className="mt-4 text-sm font-semibold text-slate-500">{label}</p></div>)}</div>
    <div className="mt-6 grid gap-6 xl:grid-cols-[1.35fr_.65fr]">
      <Card><div className="flex items-center justify-between"><h2 className="text-lg font-extrabold text-[#1a1a1a]">Continue learning</h2><button onClick={() => navigate("mylearning")} className="text-xs font-bold text-[#1a1a1a]">View all</button></div>
        {active.length === 0 ? <div className="mt-5"><Empty title="Nothing in progress" hint="Enroll in an activity to track your progress here." action={<Button variant="secondary" onClick={() => navigate("learning")}>Browse learning</Button>} /></div> :
          <div className="mt-5 space-y-5">{active.slice(0, 3).map((e) => <div className="flex gap-4" key={e.id}><span className={`grid h-12 w-12 shrink-0 place-items-center rounded-xl ${tone(e.category)}`}><Icon name={categoryIcon(e.category)} /></span><div className="min-w-0 flex-1"><div className="flex items-center justify-between gap-3"><p className="truncate text-sm font-extrabold text-slate-800">{e.title}</p><span className="text-xs font-bold">{e.progressPct}%</span></div><div className="mt-2"><ProgressBar value={e.progressPct} /></div><p className="mt-2 text-xs text-slate-400">{e.completedLessons} of {e.lessonsTotal} lessons complete{e.assessmentId && !e.assessmentPassed ? " · assessment pending" : ""}</p></div></div>)}</div>}
      </Card>
      <section className="rounded-2xl bg-[#1a1a1a] p-6 text-white"><div className="flex justify-between"><span className="rounded-full bg-[#c2a15a] px-3 py-1 text-[10px] font-extrabold uppercase tracking-wider text-[#1a1a1a]">{next ? "Next exchange" : "Virtual exchange"}</span><Icon name="globe" className="text-[#dcc690]" /></div>
        {next ? <><h2 className="mt-5 text-xl font-extrabold">{next.title}</h2><p className="mt-2 text-sm text-white/60">{fmtDay(next.startsAt)} · {fmtTime(next.startsAt)}</p><p className="mt-4 text-xs text-white/50">{next.participantCount} registered{next.countries.length ? ` · ${next.countries.join(", ")}` : ""}</p>
          {next.meetingLink && <a href={next.meetingLink} target="_blank" rel="noopener noreferrer" className="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-xl bg-[#c2a15a] px-5 text-sm font-bold text-[#1a1a1a]">Join session</a>}</> :
          <><h2 className="mt-5 text-xl font-extrabold">Meet peers across East Africa</h2><p className="mt-2 text-sm text-white/60">You have no sessions booked yet.</p><Button onClick={() => navigate("exchange")} className="mt-6 w-full">See sessions</Button></>}
      </section>
    </div>
    <div className="mt-6 grid gap-6 xl:grid-cols-[.8fr_1.2fr]">
      <Card><div className="flex items-center justify-between"><h2 className="text-lg font-extrabold text-[#1a1a1a]">My team</h2><button onClick={() => navigate("team")} className="text-xs font-bold text-[#1a1a1a]">Open team</button></div>
        {x.team ? <><p className="mt-5 text-xs font-bold uppercase tracking-wider text-[#8a6d2f]">{x.team.name}</p>{x.team.project ? <><h3 className="mt-1 font-extrabold">{x.team.project.name}</h3><div className="mt-4 flex items-center justify-between text-xs font-bold"><span>Overall progress</span><span>{x.team.project.progressPct}%</span></div><div className="mt-2"><ProgressBar value={x.team.project.progressPct} tone="bg-[#c2a15a]" /></div></> : <p className="mt-2 text-sm text-slate-500">No Impact Project yet.</p>}</> :
          <div className="mt-5"><Empty title="You're not in a team" hint="Create or join a team to start an Impact Project." /></div>}
        {x.mentors.length > 0 && <p className="mt-5 text-xs text-slate-500">Mentor: <strong>{x.mentors.map((m) => m.mentor).join(", ")}</strong></p>}
      </Card>
      <Card><div className="flex items-center justify-between"><h2 className="text-lg font-extrabold text-[#1a1a1a]">Recent messages</h2><button onClick={() => navigate("messages")} className="text-xs font-bold text-[#1a1a1a]">{x.unreadMessages > 0 ? `${x.unreadMessages} unread` : "Open inbox"}</button></div>
        {x.recentMessages.length === 0 ? <div className="mt-4"><Empty title="No conversations yet" hint="Message your mentor or teammates from their profile cards." /></div> :
          <div className="mt-4 divide-y divide-slate-100">{x.recentMessages.map((c) => <button key={c.id} onClick={() => navigate(`messages/${c.id}`)} className="flex w-full gap-3 py-3 text-left"><Avatar name={c.title} /><div className="min-w-0 flex-1"><div className="flex justify-between"><p className="truncate text-sm font-bold text-slate-800">{c.title}</p>{c.lastAt && <span className="text-[10px] text-slate-400">{ago(c.lastAt)}</span>}</div><p className="mt-0.5 truncate text-xs text-slate-500">{c.lastMessage ?? "No messages yet"}</p></div>{c.unread > 0 && <span className="grid h-5 min-w-5 place-items-center rounded-full bg-[#c2a15a] px-1 text-[10px] font-bold">{c.unread}</span>}</button>)}</div>}
      </Card>
    </div>
  </main>;
}
