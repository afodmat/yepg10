import { useEffect, type ReactNode } from "react";
import { Icon } from "./Icon";
import { initials } from "../lib/format";

export const Loading = ({ label = "Loading…" }: { label?: string }) => <div role="status" className="grid place-items-center py-12 text-sm font-semibold text-slate-400">{label}</div>;
export const ErrorNote = ({ message, onRetry }: { message: string; onRetry?: () => void }) => message ? <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{message}{onRetry && <button onClick={onRetry} className="ml-3 underline">Try again</button>}</div> : null;
export const Success = ({ message }: { message: string }) => message ? <div role="status" className="rounded-xl border border-[#efece6] bg-[#efece6] p-3 text-sm font-semibold text-[#8a6d2f]">{message}</div> : null;
export const Empty = ({ title, hint, action }: { title: string; hint?: string; action?: ReactNode }) => <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center"><p className="font-extrabold text-[#1a1a1a]">{title}</p>{hint && <p className="mt-1 text-sm text-slate-500">{hint}</p>}{action && <div className="mt-4 flex justify-center">{action}</div>}</div>;

export const ProgressBar = ({ value, tone = "bg-[#c2a15a]", height = "h-2" }: { value: number; tone?: string; height?: string }) =>
  <div className={`${height} overflow-hidden rounded-full bg-slate-100`} role="progressbar" aria-valuenow={value} aria-valuemin={0} aria-valuemax={100}><div className={`h-full rounded-full ${tone}`} style={{ width: `${Math.min(100, Math.max(0, value))}%` }} /></div>;

export const Avatar = ({ name, className = "bg-[#efece6] text-[#8a6d2f]", size = "h-10 w-10" }: { name?: string | null; className?: string; size?: string }) =>
  <span className={`grid ${size} shrink-0 place-items-center rounded-full text-xs font-extrabold ${className}`}>{initials(name)}</span>;

export const Badge = ({ children, tone = "bg-slate-100 text-slate-600" }: { children: ReactNode; tone?: string }) => <span className={`inline-block rounded-full px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider ${tone}`}>{children}</span>;
export const statusTone = (s: string) => /ACTIVE|OPEN|VALID|DONE|COMPLETED|ACCEPTED/.test(s) ? "bg-[#efece6] text-[#8a6d2f]" : /PENDING|DRAFT|SCHEDULED|SUBMITTED|IN_PROGRESS/.test(s) ? "bg-[#efe5cf] text-[#8a6d2f]" : /DISABLED|CANCELLED|REVOKED|DECLINED|ARCHIVED|CLOSED/.test(s) ? "bg-[#ffebeb] text-[#b95555]" : "bg-slate-100 text-slate-600";

export const Card = ({ children, className = "" }: { children: ReactNode; className?: string }) => <section className={`rounded-2xl border border-slate-200 bg-white p-6 ${className}`}>{children}</section>;
export const PageTitle = ({ eyebrow, title, sub, action }: { eyebrow: string; title: string; sub?: string; action?: ReactNode }) =>
  <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-sm font-bold uppercase text-[#8a6d2f]">{eyebrow}</p><h1 className="mt-1 text-3xl font-extrabold tracking-tight text-[#1a1a1a]">{title}</h1>{sub && <p className="mt-2 text-sm text-slate-500">{sub}</p>}</div>{action}</div>;

export function Modal({ title, onClose, children, wide = false }: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  useEffect(() => { const k = (e: KeyboardEvent) => e.key === "Escape" && onClose(); addEventListener("keydown", k); return () => removeEventListener("keydown", k); }, [onClose]);
  return <div className="fixed inset-0 z-[60] grid place-items-center overflow-y-auto bg-[#111111]/70 p-4" role="dialog" aria-modal="true" aria-label={title}>
    <div className={`relative w-full ${wide ? "max-w-3xl" : "max-w-md"} rounded-2xl bg-white p-6 shadow-2xl`}>
      <div className="flex items-start justify-between gap-4"><h2 className="text-xl font-extrabold text-[#1a1a1a]">{title}</h2><button onClick={onClose} aria-label="Close" className="rounded-full bg-slate-100 p-2"><Icon name="close" size={16} /></button></div>
      <div className="mt-4">{children}</div>
    </div></div>;
}

export function Confirm({ title, message, confirmLabel = "Confirm", danger = false, onConfirm, onClose }: { title: string; message: string; confirmLabel?: string; danger?: boolean; onConfirm: () => void; onClose: () => void }) {
  return <Modal title={title} onClose={onClose}><p className="text-sm leading-6 text-slate-600">{message}</p>
    <div className="mt-6 flex justify-end gap-3"><button onClick={onClose} className="min-h-10 rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-600">Cancel</button>
      <button onClick={() => { onConfirm(); onClose(); }} className={`min-h-10 rounded-xl px-4 text-sm font-bold text-white ${danger ? "bg-red-600 hover:bg-red-700" : "bg-[#1a1a1a] hover:bg-[#1a1a1a]"}`}>{confirmLabel}</button></div></Modal>;
}

export function Pagination({ page, totalPages, total, onPage }: { page: number; totalPages: number; total: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return <p className="mt-4 text-xs font-semibold text-slate-400">{total} result{total === 1 ? "" : "s"}</p>;
  const btn = "min-h-9 rounded-lg border border-slate-200 bg-white px-3 text-sm font-bold text-slate-600 disabled:opacity-40";
  return <div className="mt-4 flex items-center justify-between"><p className="text-xs font-semibold text-slate-400">{total} results · page {page + 1} of {totalPages}</p>
    <div className="flex gap-2"><button className={btn} disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</button><button className={btn} disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</button></div></div>;
}

export interface Column<T> { header: string; cell: (row: T) => ReactNode; className?: string; sortKey?: string }
export function DataTable<T>({ rows, columns, rowKey, sort, dir, onSort }: { rows: T[]; columns: Column<T>[]; rowKey: (r: T) => string | number; sort?: string; dir?: string; onSort?: (key: string) => void }) {
  return <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white"><table className="w-full text-left text-sm">
    <thead className="bg-slate-50 text-xs font-extrabold uppercase tracking-wider text-slate-500"><tr>{columns.map((c) => <th key={c.header} className={`px-4 py-3 ${c.className ?? ""}`}>
      {c.sortKey && onSort ? <button onClick={() => onSort(c.sortKey!)} className="uppercase tracking-wider">{c.header}{sort === c.sortKey ? (dir === "asc" ? " ▲" : " ▼") : ""}</button> : c.header}</th>)}</tr></thead>
    <tbody className="divide-y divide-slate-100">{rows.map((r) => <tr key={rowKey(r)} className="align-middle">{columns.map((c) => <td key={c.header} className={`px-4 py-3 ${c.className ?? ""}`}>{c.cell(r)}</td>)}</tr>)}
      {rows.length === 0 && <tr><td colSpan={columns.length} className="px-4 py-10 text-center text-slate-400">Nothing to show yet.</td></tr>}</tbody></table></div>;
}

export const inputCls = "h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:ring-2 focus:ring-[#c2a15a]";
export const areaCls = "min-h-24 w-full rounded-xl border border-slate-200 bg-white p-3 text-sm outline-none focus:ring-2 focus:ring-[#c2a15a]";
export const Labeled = ({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) => <label className="block text-sm font-bold text-slate-700">{label}<div className="mt-1.5 font-normal">{children}</div>{hint && <span className="mt-1 block text-xs font-normal text-slate-400">{hint}</span>}</label>;
