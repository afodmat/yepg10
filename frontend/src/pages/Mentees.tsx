import { Button } from "../components/ui";
import { Avatar, Badge, Card, Empty, ErrorNote, Loading, PageTitle, ProgressBar } from "../components/common";
import { mentorship, messaging } from "../api/endpoints";
import { useAction, useApi } from "../hooks/useApi";
import { ago, fmtDate } from "../lib/format";
import { navigate } from "../lib/nav";

/** Mentor home: incoming requests and assigned mentees (participants and teams) with progress. */
export function Mentees() {
  const incoming = useApi(() => mentorship.incoming());
  const mine = useApi(() => mentorship.mine());
  const action = useAction();
  const respond = async (id: number, accept: boolean) => { await action.run(() => mentorship.respond(id, accept)); incoming.reload(); mine.reload(); };
  const message = async (userId: number) => { const c = await action.run(() => messaging.direct(userId)); if (c) navigate(`messages/${c.id}`); };
  return <main className="p-5 lg:p-9"><PageTitle eyebrow="Mentor" title="Your mentees" sub="Review progress, give feedback on projects and stay in touch." />
    <div className="mt-4"><ErrorNote message={action.error || incoming.error || mine.error} /></div>
    {incoming.data && incoming.data.length > 0 && <Card className="mt-4"><h2 className="text-lg font-extrabold">Mentorship requests</h2><div className="mt-4 space-y-3">{incoming.data.map((r) => <div key={r.id} className="flex flex-col gap-3 rounded-xl bg-slate-50 p-4 sm:flex-row sm:items-center"><Avatar name={r.participant} /><div className="min-w-0 flex-1"><p className="font-bold">{r.participant} <span className="text-xs font-normal text-slate-400">{ago(r.createdAt)}</span></p>{r.participantCountry && <p className="mt-1 text-xs text-slate-500">From {r.participantCountry}</p>}{r.message && <p className="mt-1 text-sm text-slate-600">{r.message}</p>}</div><div className="flex gap-2"><Button variant="secondary" onClick={() => respond(r.id, true)}>Accept</Button><Button variant="ghost" onClick={() => respond(r.id, false)}>Decline</Button></div></div>)}</div></Card>}
    <h2 className="mt-8 text-lg font-extrabold">Assigned to you</h2>
    {mine.loading && !mine.data ? <Loading /> : mine.data?.length === 0 ? <div className="mt-3"><Empty title="No mentees yet" hint="Facilitators assign mentees, or participants can send you a request from the mentor directory." /></div> :
      <div className="mt-3 grid gap-4 md:grid-cols-2">{mine.data?.map((a) => <Card key={a.id}><div className="flex items-center gap-3"><Avatar name={a.mentee} /><div><p className="font-extrabold">{a.mentee}</p><Badge>{a.type === "TEAM" ? "Team" : "Participant"}</Badge>{a.type === "PARTICIPANT" && a.country && <p className="mt-1 text-xs text-slate-500">From {a.country}</p>}</div></div>
        <p className="mt-4 text-xs font-bold text-slate-500">{a.type === "TEAM" ? "Project progress" : "Learning progress"}</p><div className="mt-2 flex items-center gap-3"><div className="flex-1"><ProgressBar value={(a.type === "TEAM" ? a.projectProgress : a.learningProgress) ?? 0} tone="bg-[#c2a15a]" /></div><span className="text-sm font-bold">{(a.type === "TEAM" ? a.projectProgress : a.learningProgress) ?? 0}%</span></div>
        <p className="mt-3 text-xs text-slate-400">Since {fmtDate(a.createdAt)}</p>{a.participantId && <Button variant="ghost" className="mt-4" onClick={() => message(a.participantId!)}>Message</Button>}</Card>)}</div>}
  </main>;
}
