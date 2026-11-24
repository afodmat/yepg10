import type { IconName } from "../types";

export const initials = (name?: string | null) => (name ?? "?").split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]).join("").toUpperCase();
export const fmtDate = (iso?: string | null) => iso ? new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" }) : "";
export const fmtDay = (iso?: string | null) => iso ? new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short" }) : "";
export const fmtTime = (iso?: string | null) => iso ? new Date(iso).toLocaleTimeString("en-GB", { hour: "numeric", minute: "2-digit", hour12: true, timeZone: "Africa/Kampala" }) + " EAT" : "";
export const fmtDateTime = (iso?: string | null) => iso ? `${fmtDate(iso)}, ${fmtTime(iso)}` : "";
export const ago = (iso: string) => {
  const m = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (m < 1) return "just now"; if (m < 60) return `${m}m ago`; if (m < 1440) return `${Math.round(m / 60)}h ago`; return fmtDay(iso);
};
export const dateRange = (a?: string | null, b?: string | null) => a && b ? `${fmtDay(a)} – ${fmtDay(b)}` : a ? `From ${fmtDay(a)}` : "Self-paced";

const palette = ["bg-[#efece6] text-[#1a1a1a]", "bg-[#efe5cf] text-[#8a6d2f]", "bg-[#efece6] text-[#8a6d2f]", "bg-[#eeebff] text-[#6652bf]", "bg-[#ffebeb] text-[#b95555]"];
export const tone = (key: string) => palette[[...key].reduce((n, c) => n + c.charCodeAt(0), 0) % palette.length];
export const categoryIcon = (c: string): IconName => /ai|digital/i.test(c) ? "spark" : /exchange|intercultural/i.test(c) ? "globe" : /media|critical/i.test(c) ? "search" : /employ|leader/i.test(c) ? "briefcase" : "book";
export const CATEGORIES = ["Digital Literacy", "AI Literacy", "Critical Thinking", "Media Literacy", "Intercultural Communication", "Employability", "Leadership"];
export const PROJECT_CATEGORIES = ["Employment", "Education", "Agriculture", "Health", "Climate Change", "Digital Inclusion"];
