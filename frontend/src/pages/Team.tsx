import { FormEvent, useState } from "react";
import { Icon } from "../components/Icon";
import { Button } from "../components/ui";
import { Avatar, Badge, Card, Confirm, Empty, ErrorNote, Labeled, Loading, PageTitle, Pagination, ProgressBar, Success, areaCls, inputCls, statusTone } from "../components/common";
import { account, fileUrl, messaging, projects, teams, type Project, type Team } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";
import { useAction, useApi } from "../hooks/useApi";
import { PROJECT_CATEGORIES, ago, fmtDate } from "../lib/format";
import { navigate } from "../lib/nav";

const colors = ["bg-[#c2a15a]", "bg-[#c2a15a]", "bg-[#e87979]", "bg-[#9c8ce7]", "bg-[#8a8a85]", "bg-[#efe5cf]"];

function NoTeam({ onJoined }: { onJoined: () => void }) {
  const [form, setForm] = useState({ name: "", description: "", country: "Uganda", institution: "", category: "", open: true });
  const [page, setPage] = useState(0);
  const open = useApi(() => teams.list({ page, size: 6 }), [page]);
  const inv = useApi(() => teams.invitations());
  const action = useAction();
  const create = async (e: FormEvent) => { e.preventDefault(); if (await action.run(() => teams.create(form))) onJoined(); };
  return <div className="mt-6 grid gap-6 xl:grid-cols-2">
    <div className="space-y-6">
      {inv.data && inv.data.length > 0 && <Card><h2 className="text-lg font-extrabold">Invitations</h2><div className="mt-4 space-y-3">{inv.data.map((i) => <div key={i.id} className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 p-3"><div><p className="text-sm font-bold">{i.teamName}</p><p className="text-xs text-slate-500">Invited by {i.invitedBy}</p></div>
        <div className="flex gap-2"><Button variant="secondary" onClick={async () => { if (await action.run(() => teams.accept(i.id))) onJoined(); }}>Accept</Button><Button variant="ghost" onClick={async () => { await action.run(() => teams.decline(i.id)); inv.reload(); }}>Decline</Button></div></div>)}</div></Card>}
      <Card><h2 className="text-lg font-extrabold">Join an open team</h2><div className="mt-4"><ErrorNote message={open.error} />{open.loading && !open.data ? <Loading /> : open.data?.items.length === 0 ? <Empty title="No open teams right now" hint="Create your own team on the right." /> :
        <div className="space-y-3">{open.data?.items.map((t) => <div key={t.id} className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 p-4"><div className="min-w-0"><p className="font-bold">{t.name}</p><p className="truncate text-xs text-slate-500">{t.category ?? "General"} · {t.memberCount} members · led by {t.leaderName}</p></div><Button variant="secondary" onClick={async () => { if (await action.run(() => teams.join(t.id))) onJoined(); }}>Join</Button></div>)}</div>}
        {open.data && <Pagination page={open.data.page} totalPages={open.data.totalPages} total={open.data.total} onPage={setPage} />}</div></Card>
    </div>
    <Card><h2 className="text-lg font-extrabold">Create a team</h2>
      <form onSubmit={create} className="mt-4 space-y-4">
        <Labeled label="Team name"><input required minLength={3} maxLength={80} className={inputCls} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Labeled>
        <Labeled label="What will you work on?"><textarea maxLength={1000} className={areaCls} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Labeled>
        <div className="grid gap-4 sm:grid-cols-2"><Labeled label="Country"><input className={inputCls} value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} /></Labeled><Labeled label="Institution"><input className={inputCls} value={form.institution} onChange={(e) => setForm({ ...form, institution: e.target.value })} /></Labeled></div>
        <Labeled label="Focus area"><select className={inputCls} value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}><option value="">Choose…</option>{PROJECT_CATEGORIES.map((c) => <option key={c}>{c}</option>)}</select></Labeled>
        <label className="flex items-center gap-2 text-sm font-semibold text-slate-600"><input type="checkbox" checked={form.open} onChange={(e) => setForm({ ...form, open: e.target.checked })} /> Anyone can join (otherwise invite-only)</label>
        <ErrorNote message={action.error} /><Button type="submit" variant="secondary">{action.busy ? "Creating…" : "Create team"}</Button></form></Card>
  </div>;
}

function ProjectPanel({ team, project, reload, canLead }: { team: Team; project?: Project | null; reload: () => void; canLead: boolean }) {
  const [form, setForm] = useState({ name: "", description: "", problemStatement: "", solution: "", category: "Employment", location: "" });
  const [update, setUpdate] = useState({ content: "", milestoneId: "", status: "DONE" });
  const [comment, setComment] = useState("");
  const action = useAction();
  const current = project ?? null;

  if (!current) return canLead ?
    <Card><h2 className="text-lg font-extrabold">Start your Impact Project</h2><p className="mt-1 text-sm text-slate-500">Describe the problem you want to solve. Six milestones are created for you.</p>
      <form className="mt-5 space-y-4" onSubmit={async (e) => { e.preventDefault(); if (await action.run(() => projects.create({ ...form, teamId: team.id }))) reload(); }}>
        <Labeled label="Project name"><input required className={inputCls} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Labeled>
        <Labeled label="Description"><textarea required className={areaCls} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Labeled>
        <Labeled label="Problem statement"><textarea className={areaCls} value={form.problemStatement} onChange={(e) => setForm({ ...form, problemStatement: e.target.value })} /></Labeled>
        <Labeled label="Proposed solution"><textarea className={areaCls} value={form.solution} onChange={(e) => setForm({ ...form, solution: e.target.value })} /></Labeled>
        <div className="grid gap-4 sm:grid-cols-2"><Labeled label="Category"><select className={inputCls} value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>{PROJECT_CATEGORIES.map((c) => <option key={c}>{c}</option>)}</select></Labeled><Labeled label="Location"><input className={inputCls} value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></Labeled></div>
        <ErrorNote message={action.error} /><Button type="submit" variant="secondary">{action.busy ? "Creating…" : "Create project"}</Button></form></Card> :
    <Card><Empty title="No project yet" hint="Your team leader can start the Impact Project." /></Card>;

  const next = current.milestones.find((m) => m.status !== "DONE");
  return <><Card>
    <div className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="text-lg font-extrabold">{current.name}</h2><p className="mt-1 text-xs text-slate-500">{current.category}{current.location ? ` · ${current.location}` : ""}</p></div><Badge tone={statusTone(current.status)}>{current.status.toLowerCase()}</Badge></div>
    <p className="mt-3 text-sm leading-6 text-slate-500">{current.description}</p>
    {current.problemStatement && <p className="mt-3 text-sm"><strong>Problem:</strong> <span className="text-slate-600">{current.problemStatement}</span></p>}
    {current.solution && <p className="mt-2 text-sm"><strong>Solution:</strong> <span className="text-slate-600">{current.solution}</span></p>}
    <div className="mt-6 flex items-center justify-between text-sm font-bold"><span>Project progress</span><span>{current.progressPct}%</span></div><div className="mt-2"><ProgressBar value={current.progressPct} tone="bg-[#c2a15a]" height="h-3" /></div>
    {next && <p className="mt-2 text-xs text-slate-500">Next milestone: <strong>{next.name}</strong></p>}
    <ol className="mt-6 grid gap-3 sm:grid-cols-3">{current.milestones.map((m, i) => <li key={m.id} className={`rounded-xl p-4 ${m.status === "DONE" ? "bg-[#efece6]" : m.status === "IN_PROGRESS" ? "bg-[#efe5cf]" : "bg-slate-50"}`}>
      <p className="text-xs font-bold text-slate-500">{i + 1}. {m.name}</p>
      {current.canContribute ? <select aria-label={`Status of ${m.name}`} value={m.status} onChange={async (e) => { await action.run(() => projects.milestone(current.id, m.id, e.target.value)); reload(); }} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-2 py-1 text-sm font-bold"><option value="PENDING">Not started</option><option value="IN_PROGRESS">In progress</option><option value="DONE">Complete</option></select> : <p className="mt-1 text-sm font-bold">{m.status.replace("_", " ").toLowerCase()}</p>}</li>)}</ol>
    <div className="mt-4"><ErrorNote message={action.error} /></div></Card>

  {current.canContribute && <Card className="mt-6"><h2 className="text-lg font-extrabold">Submit a progress update</h2>
    <form className="mt-4 space-y-3" onSubmit={async (e) => { e.preventDefault(); if (await action.run(() => projects.postUpdate(current.id, update.content, update.milestoneId ? Number(update.milestoneId) : undefined, update.milestoneId ? update.status : undefined), "Update sent to your mentor.")) { setUpdate({ content: "", milestoneId: "", status: "DONE" }); reload(); } }}>
      <textarea required maxLength={2000} aria-label="Update" className={areaCls} placeholder="What did the team do this week?" value={update.content} onChange={(e) => setUpdate({ ...update, content: e.target.value })} />
      <div className="flex flex-wrap items-center gap-3"><select aria-label="Milestone" className={`${inputCls} sm:w-56`} value={update.milestoneId} onChange={(e) => setUpdate({ ...update, milestoneId: e.target.value })}><option value="">No milestone change</option>{current.milestones.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}</select>
        {update.milestoneId && <select aria-label="New status" className={`${inputCls} sm:w-40`} value={update.status} onChange={(e) => setUpdate({ ...update, status: e.target.value })}><option value="IN_PROGRESS">In progress</option><option value="DONE">Complete</option><option value="PENDING">Not started</option></select>}
        <Button type="submit" variant="secondary">Send update <Icon name="arrow" size={16} /></Button></div><Success message={action.ok} /></form>
    <div className="mt-5 flex flex-wrap items-center gap-3"><label className="inline-flex min-h-10 cursor-pointer items-center rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-600">Attach a file<input type="file" className="sr-only" accept=".pdf,.docx,.pptx,.xlsx,.txt,.csv,.png,.jpg,.jpeg" onChange={async (e) => { const f = e.target.files?.[0]; if (!f) return; const up = await action.run(() => account.upload(f, f.type.startsWith("image/") ? "PROJECT_IMAGE" : "PROJECT_DOC")); if (up) { await action.run(() => projects.attach(current.id, up.id)); reload(); } e.target.value = ""; }} /></label>
      {current.attachments.map((a) => <a key={a.fileId} href={fileUrl(a.fileId)} className="text-sm font-semibold text-[#1a1a1a] underline" target="_blank" rel="noopener noreferrer">{a.name}</a>)}</div></Card>}

  {current.canView && <Card className="mt-6"><h2 className="text-lg font-extrabold">Updates and feedback</h2>
    <div className="mt-4 space-y-5">{[...current.updates.map((u) => ({ ...u, kind: "Update" })), ...current.comments.map((c) => ({ ...c, kind: "Feedback" }))].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).map((p) => <div key={`${p.kind}${p.id}`} className="flex gap-3"><Avatar name={p.author} /><div className="min-w-0"><p className="text-sm font-bold">{p.author} <span className={`ml-1 rounded-full px-2 py-0.5 text-[10px] ${p.kind === "Feedback" ? "bg-[#efe5cf] text-[#8a6d2f]" : "bg-[#efece6] text-[#1a1a1a]"}`}>{p.kind}</span> <span className="text-xs font-normal text-slate-400">{ago(p.createdAt)}</span></p><p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-slate-600">{p.content}</p></div></div>)}
      {current.updates.length + current.comments.length === 0 && <p className="text-sm text-slate-400">Nothing yet. Post the first update!</p>}</div>
    <form className="mt-5 flex gap-3" onSubmit={async (e) => { e.preventDefault(); if (await action.run(() => projects.comment(current.id, comment))) { setComment(""); reload(); } }}><input required maxLength={2000} aria-label="Comment" className={inputCls} placeholder="Add a comment…" value={comment} onChange={(e) => setComment(e.target.value)} /><Button type="submit" variant="ghost">Comment</Button></form></Card>}</>;
}

export function TeamPage() {
  const { user } = useAuth();
  const mine = useApi(() => teams.list({ mine: true, size: 5 }));
  const mineTeam = mine.data?.items.find((t) => t.status === "ACTIVE");
  const detail = useApi(async () => mineTeam ? teams.get(mineTeam.id) : null, [mineTeam?.id]);
  const team = detail.data;
  const proj = useApi(async () => team?.project ? projects.get(team.project.id) : null, [team?.project?.id, team?.id]);
  const action = useAction();
  const [invite, setInvite] = useState("");
  const [leave, setLeave] = useState(false);
  const [remove, setRemove] = useState<number | null>(null);
  const isLeader = !!team && team.leaderId === user?.id;
  const reloadAll = () => { mine.reload(); detail.reload(); proj.reload(); };

  const message = async (userId: number) => { const c = await action.run(() => messaging.direct(userId)); if (c) navigate(`messages/${c.id}`); };

  if ((mine.loading && !mine.data) || (detail.loading && !detail.data && mineTeam)) return <main className="p-5 lg:p-9"><Loading /></main>;
  if (!team) return <main className="p-5 lg:p-9"><PageTitle eyebrow="My team" title="Find your team" sub="Teams work together on an Impact Project with a mentor's support." /><div className="mt-4"><ErrorNote message={mine.error || action.error} /></div><NoTeam onJoined={reloadAll} /></main>;

  return <main className="p-5 lg:p-9">
    <PageTitle eyebrow="My team" title={team.name} sub={`${team.category ?? "General"} · ${team.country ?? ""} · ${team.status.toLowerCase()}`} action={<Button variant="ghost" onClick={() => setLeave(true)}>Leave team</Button>} />
    <div className="mt-4"><ErrorNote message={action.error} /><Success message={action.ok} /></div>
    <div className="mt-5 grid gap-6 xl:grid-cols-[1.3fr_.7fr]">
      <div><ProjectPanel team={team} project={proj.data} reload={reloadAll} canLead={isLeader} /></div>
      <aside className="space-y-6">
        <div className="rounded-2xl bg-[#1a1a1a] p-6 text-white"><p className="text-xs font-bold uppercase tracking-wider text-[#dcc690]">Your mentor</p>
          {team.mentors && team.mentors.length ? <p className="mt-4 text-lg font-extrabold">{team.mentors.join(", ")}</p> : <p className="mt-4 text-sm text-white/70">No mentor assigned yet. Ask for one from the Mentorship page.</p>}
          <Button onClick={() => navigate("mentorship")} variant="ghost" className="mt-5 w-full border-white/20 bg-white/5 text-white hover:text-white">Browse mentors</Button></div>
        <Card><h2 className="text-lg font-extrabold">Team members ({team.memberCount})</h2>
          <div className="mt-4 space-y-3">{team.members?.map((m, i) => <div key={m.userId} className="flex items-center gap-3 rounded-xl bg-slate-50 p-3"><Avatar name={m.name} className={`${colors[i % colors.length]} text-[#1a1a1a]`} /><div className="min-w-0 flex-1"><p className="truncate text-sm font-bold">{m.name}</p><p className="text-xs text-slate-500">{m.role === "LEADER" ? "Team leader" : m.title ?? "Member"}{m.country ? ` · From ${m.country}` : ""}</p></div>
            {m.userId !== user?.id && <button aria-label={`Message ${m.name}`} onClick={() => message(m.userId)} className="p-1.5 text-[#1a1a1a]"><Icon name="message" size={16} /></button>}
            {isLeader && m.role !== "LEADER" && <button onClick={() => setRemove(m.userId)} className="text-xs font-bold text-slate-400 hover:text-red-600">Remove</button>}</div>)}</div>
          {isLeader && <form className="mt-4 flex gap-2" onSubmit={async (e) => { e.preventDefault(); if (await action.run(() => teams.invite(team.id, invite), "Invitation sent.")) setInvite(""); }}><input required type="email" aria-label="Invite by email" className={inputCls} placeholder="teammate@email.com" value={invite} onChange={(e) => setInvite(e.target.value)} /><Button type="submit" variant="ghost">Invite</Button></form>}
          <p className="mt-3 text-xs text-slate-400">Created {fmtDate(team.createdAt)}</p></Card>
      </aside>
    </div>
    {leave && <Confirm danger title="Leave this team?" message={isLeader ? "You are the team leader. Leadership will pass to the longest-standing member." : "You will lose access to the team's project and chat."} confirmLabel="Leave team" onClose={() => setLeave(false)} onConfirm={async () => { await action.run(() => teams.leave(team.id)); reloadAll(); }} />}
    {remove !== null && <Confirm danger title="Remove member?" message="They will lose access to the team's project and chat." confirmLabel="Remove" onClose={() => setRemove(null)} onConfirm={async () => { await action.run(() => teams.remove(team.id, remove)); reloadAll(); }} />}
  </main>;
}
