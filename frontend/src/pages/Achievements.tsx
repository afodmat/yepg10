import { useState } from "react";
import { Icon } from "../components/Icon";
import { Button, Logo } from "../components/ui";
import { Badge, Card, Empty, ErrorNote, Loading, Modal, PageTitle, statusTone } from "../components/common";
import { recognition, type Certificate } from "../api/endpoints";
import { useApi } from "../hooks/useApi";
import { fmtDate } from "../lib/format";
import type { IconName } from "../types";

const icons: Record<string, IconName> = { spark: "spark", trophy: "trophy", globe: "globe", team: "team", message: "message", leaf: "leaf" };
const tones = ["bg-[#efe5cf] text-[#8a6d2f]", "bg-[#efece6] text-[#1a1a1a]", "bg-[#efece6] text-[#8a6d2f]", "bg-[#eeebff] text-[#6652bf]"];

export function Achievements() {
  const ach = useApi(() => recognition.achievements());
  const certs = useApi(() => recognition.certificates());
  const [view, setView] = useState<Certificate | null>(null);
  const earned = ach.data?.filter((a) => a.earned).length ?? 0;
  return <main className="p-5 lg:p-9">
    <PageTitle eyebrow="Your progress" title="Achievements" sub={ach.data ? `${earned} of ${ach.data.length} unlocked` : undefined} />
    <div className="mt-6"><ErrorNote message={ach.error} onRetry={ach.reload} /></div>
    {ach.loading && !ach.data ? <Loading /> : <div className="mt-4 grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{ach.data?.map((a, i) => <div key={a.id} className={`rounded-2xl border bg-white p-6 ${a.earned ? "border-slate-200" : "border-dashed border-slate-300 opacity-70"}`}>
      <span className={`grid h-14 w-14 place-items-center rounded-2xl ${a.earned ? tones[i % 4] : "bg-slate-100 text-slate-400"}`}><Icon name={icons[a.icon] ?? "trophy"} size={27} /></span>
      <h2 className="mt-5 text-lg font-extrabold">{a.name}</h2><p className="mt-2 text-sm leading-6 text-slate-500">{a.description}</p>
      <p className="mt-4 text-xs font-bold text-[#8a6d2f]">{a.earned ? `Awarded ${fmtDate(a.awardedAt)}` : `How to earn: ${a.criteria}`}</p></div>)}</div>}
    <h2 className="mt-10 text-xl font-extrabold text-[#1a1a1a]">Certificates</h2>
    <div className="mt-4 space-y-4"><ErrorNote message={certs.error} />
      {certs.data?.length === 0 && <Empty title="No certificates yet" hint="Complete an activity (and pass its assessment) to earn one." />}
      {certs.data?.map((c) => <Card key={c.id}><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"><div><p className="text-xs font-bold uppercase tracking-wider text-[#8a6d2f]">Certificate of completion <Badge tone={statusTone(c.status)}>{c.status.toLowerCase()}</Badge></p><h3 className="mt-2 text-xl font-extrabold">{c.activityTitle}</h3><p className="mt-1 text-sm text-slate-500">No. {c.number} · Issued {fmtDate(c.issuedAt)}</p></div>
        <div className="flex gap-2"><Button variant="ghost" onClick={() => setView(c)}>Preview</Button>{c.status === "VALID" && <a href={`/api/certificates/${c.id}/pdf`} className="inline-flex min-h-11 items-center rounded-xl bg-[#c2a15a] px-5 text-sm font-bold text-[#1a1a1a]">Download PDF</a>}</div></div></Card>)}</div>
    {view && <Modal title="Certificate" onClose={() => setView(null)} wide><div className="certificate-border p-8 text-center sm:p-12"><div className="flex justify-center"><Logo /></div><p className="mt-8 text-xs font-bold uppercase tracking-[.32em] text-[#8a6d2f]">Certificate of Completion</p><h2 className="mt-5 text-3xl font-serif text-[#1a1a1a] sm:text-4xl">{view.issuer}</h2><p className="mt-6 text-sm text-slate-500">This certifies that</p><p className="mt-2 text-3xl font-bold text-[#1a1a1a]">{view.holderName}</p><p className="mt-4 text-sm text-slate-500">has successfully completed</p><p className="mt-2 text-xl font-extrabold text-[#1a1a1a]">{view.activityTitle}</p>
      <p className="mt-8 text-xs text-slate-500">Certificate no. {view.number} · Issued {fmtDate(view.issuedAt)}</p><p className="mt-1 text-xs text-slate-400">Verify at {location.origin}/#verify/{view.verificationCode}</p></div></Modal>}
  </main>;
}
