import { FormEvent, useEffect, useRef, useState } from "react";
import { Icon } from "../components/Icon";
import { Avatar, ErrorNote, Loading } from "../components/common";
import { messaging, type Message } from "../api/endpoints";
import { useApi, useAction } from "../hooks/useApi";
import { useRoute, navigate } from "../lib/nav";
import { ago } from "../lib/format";

export function Messages({ onRead }: { onRead: () => void }) {
  const route = useRoute();
  const convs = useApi(() => messaging.list());
  const [search, setSearch] = useState("");
  const selected = route.param ? Number(route.param) : convs.data?.[0]?.id;
  const [msgs, setMsgs] = useState<Message[]>([]);
  const [text, setText] = useState("");
  const [loading, setLoading] = useState(false);
  const action = useAction();
  const bottom = useRef<HTMLDivElement>(null);
  const lastId = useRef(0);

  // Initial load of a conversation, then poll for new messages (cheap `after` query) while the tab is visible.
  useEffect(() => {
    if (!selected) return;
    let live = true;
    setMsgs([]); lastId.current = 0; setLoading(true);
    const pull = async () => {
      if (document.hidden) return;
      try {
        const fresh = await messaging.messages(selected, lastId.current || undefined);
        if (!live || fresh.length === 0) return;
        lastId.current = fresh[fresh.length - 1].id;
        setMsgs((m) => (lastId.current && m.length ? [...m, ...fresh.filter((f) => !m.some((x) => x.id === f.id))] : fresh));
        messaging.read(selected).then(() => { convs.reload(); onRead(); }).catch(() => undefined);
      } catch { /* transient network error: next poll retries */ }
    };
    pull().finally(() => live && setLoading(false));
    const t = setInterval(pull, 8000);
    return () => { live = false; clearInterval(t); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected]);
  useEffect(() => { bottom.current?.scrollIntoView({ block: "end" }); }, [msgs.length]);

  const send = async (e: FormEvent) => {
    e.preventDefault();
    if (!selected || !text.trim()) return;
    const sent = await action.run(() => messaging.send(selected, text));
    if (sent) { setText(""); lastId.current = sent.id; setMsgs((m) => [...m, sent]); convs.reload(); }
  };

  const list = (convs.data ?? []).filter((c) => c.title.toLowerCase().includes(search.toLowerCase()));
  const current = convs.data?.find((c) => c.id === selected);

  return <main className="p-5 lg:p-9"><p className="text-sm font-bold uppercase text-[#8a6d2f]">Communication</p><h1 className="mt-1 text-3xl font-extrabold text-[#1a1a1a]">Messages</h1>
    <div className="mt-4"><ErrorNote message={convs.error} onRetry={convs.reload} /></div>
    <div className="mt-4 grid min-h-[560px] overflow-hidden rounded-2xl border border-slate-200 bg-white lg:grid-cols-[320px_1fr]">
      <div className="border-b border-slate-200 lg:border-b-0 lg:border-r"><div className="p-4"><label className="relative block"><span className="sr-only">Search conversations</span><Icon name="search" size={16} className="absolute left-3 top-3 text-slate-400" /><input value={search} onChange={(e) => setSearch(e.target.value)} className="h-10 w-full rounded-xl bg-slate-100 pl-9 pr-3 text-sm outline-none" placeholder="Search conversations" /></label></div>
        {convs.loading && !convs.data && <Loading />}
        {list.map((c) => <button key={c.id} onClick={() => navigate(`messages/${c.id}`)} className={`flex w-full gap-3 border-t border-slate-100 p-4 text-left ${selected === c.id ? "bg-[#efece6]" : ""}`}><Avatar name={c.title} /><div className="min-w-0 flex-1"><div className="flex justify-between gap-2"><p className="truncate text-sm font-bold">{c.title}</p>{c.lastAt && <span className="shrink-0 text-[10px] text-slate-400">{ago(c.lastAt)}</span>}</div><p className="mt-1 truncate text-xs text-slate-500">{c.lastMessage ?? c.subtitle}</p></div>{c.unread > 0 && <span className="grid h-5 min-w-5 place-items-center self-center rounded-full bg-[#c2a15a] px-1 text-[10px] font-bold">{c.unread}</span>}</button>)}
        {convs.data?.length === 0 && <p className="p-6 text-sm text-slate-500">No conversations yet. Start one from a mentor or teammate.</p>}</div>
      <div className="flex min-h-[420px] flex-col">{!current ? <div className="grid flex-1 place-items-center p-8 text-sm text-slate-400">Select a conversation</div> : <>
        <div className="border-b border-slate-200 p-5"><p className="font-extrabold">{current.title}</p><p className="text-xs text-[#8a6d2f]">{current.subtitle}</p></div>
        <div className="flex-1 space-y-3 overflow-y-auto bg-slate-50 p-5" style={{ maxHeight: 460 }} aria-live="polite">{loading && <Loading />}
          {msgs.map((m) => <div key={m.id} className={`max-w-md ${m.mine ? "ml-auto" : ""}`}>{!m.mine && current.type === "TEAM" && <p className="mb-1 text-[11px] font-bold text-slate-400">{m.sender}</p>}<div className={`rounded-2xl p-4 text-sm leading-6 ${m.mine ? "rounded-tr-sm bg-[#1a1a1a] text-white" : "rounded-tl-sm bg-white shadow-sm"}`}><span className="whitespace-pre-wrap break-words">{m.body}</span></div><p className={`mt-1 text-[10px] text-slate-400 ${m.mine ? "text-right" : ""}`}>{ago(m.createdAt)}</p></div>)}
          {!loading && msgs.length === 0 && <p className="text-center text-sm text-slate-400">No messages yet. Say hello!</p>}<div ref={bottom} /></div>
        <form onSubmit={send} className="flex gap-3 border-t border-slate-200 p-4"><input aria-label="Write a message" maxLength={2000} value={text} onChange={(e) => setText(e.target.value)} className="h-11 flex-1 rounded-xl border border-slate-200 px-4 text-sm outline-none focus:ring-2 focus:ring-[#c2a15a]" placeholder="Write a message…" /><button type="submit" disabled={action.busy} className="rounded-xl bg-[#1a1a1a] px-5 text-sm font-bold text-white disabled:opacity-50">Send</button></form>
        {action.error && <p role="alert" className="px-4 pb-3 text-xs font-semibold text-red-600">{action.error}</p>}</>}</div>
    </div></main>;
}
