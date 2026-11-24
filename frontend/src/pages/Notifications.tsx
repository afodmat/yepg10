import { useState } from "react";
import { Icon } from "../components/Icon";
import { Button } from "../components/ui";
import { Card, Empty, ErrorNote, Loading, PageTitle, Pagination } from "../components/common";
import { notices } from "../api/endpoints";
import { useApi } from "../hooks/useApi";
import { ago } from "../lib/format";
import { navigate } from "../lib/nav";
import type { IconName } from "../types";

const icon = (t: string): IconName => ({ CERTIFICATE: "trophy", ACHIEVEMENT: "trophy", MENTOR_MESSAGE: "mail", TEAM_INVITATION: "team", PROJECT_UPDATE: "leaf", ASSESSMENT_RESULT: "check", ENROLLMENT: "book", LEARNING_ACTIVITY: "globe" } as Record<string, IconName>)[t] ?? "bell";

export function Notifications({ onChange }: { onChange: () => void }) {
  const [page, setPage] = useState(0);
  const list = useApi(() => notices.list(page), [page]);
  const open = async (id: number, link?: string, read?: boolean) => { if (!read) { await notices.read(id).catch(() => undefined); onChange(); } if (link) navigate(link); else list.reload(); };
  return <main className="p-5 lg:p-9"><PageTitle eyebrow="Stay informed" title="Notifications" action={<Button variant="ghost" onClick={async () => { await notices.readAll(); onChange(); list.reload(); }}>Mark all as read</Button>} />
    <div className="mt-6"><ErrorNote message={list.error} onRetry={list.reload} /></div>
    {list.loading && !list.data ? <Loading /> : list.data?.items.length === 0 ? <Empty title="You're all caught up" /> : <Card className="mt-2 !p-2"><ul className="divide-y divide-slate-100">{list.data?.items.map((n) => <li key={n.id}><button onClick={() => open(n.id, n.link, n.read)} className="flex w-full gap-3 p-4 text-left"><span className={`grid h-10 w-10 shrink-0 place-items-center rounded-full ${n.read ? "bg-slate-100 text-slate-400" : "bg-[#efece6] text-[#8a6d2f]"}`}><Icon name={icon(n.type)} size={17} /></span><div className="min-w-0 flex-1"><p className={`text-sm ${n.read ? "font-semibold text-slate-600" : "font-extrabold text-slate-900"}`}>{n.title}</p>{n.body && <p className="mt-0.5 text-xs text-slate-500">{n.body}</p>}</div><span className="shrink-0 text-[10px] text-slate-400">{ago(n.createdAt)}</span></button></li>)}</ul></Card>}
    {list.data && <Pagination page={list.data.page} totalPages={list.data.totalPages} total={list.data.total} onPage={setPage} />}
  </main>;
}
