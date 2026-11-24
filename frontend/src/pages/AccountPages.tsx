import { FormEvent, useEffect, useState } from "react";
import { Button, Field, Logo } from "../components/ui";
import { ErrorNote, Loading, Success } from "../components/common";
import { account, recognition, type Verification } from "../api/endpoints";
import { useAction, useApi } from "../hooks/useApi";
import { useRoute, navigate } from "../lib/nav";
import { fmtDate } from "../lib/format";
import type { ReactNode } from "react";

const Shell = ({ title, children }: { title: string; children: ReactNode }) => <div className="grid min-h-screen place-items-center bg-[#f6f5f2] p-5"><div className="w-full max-w-md rounded-3xl bg-white p-8 shadow-sm"><Logo /><h1 className="mt-6 text-2xl font-extrabold text-[#1a1a1a]">{title}</h1><div className="mt-5 space-y-4">{children}</div></div></div>;

export function ForgotPassword() {
  const [email, setEmail] = useState(""); const a = useAction();
  const submit = async (e: FormEvent) => { e.preventDefault(); const r = await a.run(() => account.forgot(email)); if (r) a.run(async () => r, r.message); };
  return <Shell title="Reset your password"><p className="text-sm text-slate-500">Enter your email and we'll send you a reset link.</p>
    <form onSubmit={submit} className="space-y-4"><Field label="Email address" type="email" placeholder="amina@example.com" value={email} onChange={setEmail} autoComplete="email" /><ErrorNote message={a.error} /><Success message={a.ok} /><Button type="submit" variant="secondary" className="w-full">{a.busy ? "Sending…" : "Send reset link"}</Button></form>
    <button onClick={() => navigate("login")} className="text-sm font-bold text-[#1a1a1a]">Back to log in</button></Shell>;
}

export function ResetPassword() {
  const { query } = useRoute(); const token = query.get("token") ?? "";
  const [pw, setPw] = useState(""); const [confirm, setConfirm] = useState(""); const a = useAction(); const [done, setDone] = useState(false);
  const submit = async (e: FormEvent) => { e.preventDefault(); if (pw !== confirm) return; if (await a.run(() => account.reset(token, pw))) setDone(true); };
  return <Shell title="Choose a new password">{done ? <><Success message="Password updated. You can now log in." /><Button variant="secondary" className="w-full" onClick={() => navigate("login")}>Log in</Button></> :
    <form onSubmit={submit} className="space-y-4"><Field label="New password" type="password" placeholder="At least 8 characters" value={pw} onChange={setPw} autoComplete="new-password" /><Field label="Confirm password" type="password" placeholder="Repeat password" value={confirm} onChange={setConfirm} autoComplete="new-password" />
      {confirm && pw !== confirm && <p role="alert" className="text-sm text-red-600">Passwords do not match.</p>}{!token && <ErrorNote message="This reset link is incomplete. Request a new one." />}<ErrorNote message={a.error} /><Button type="submit" variant="secondary" className="w-full">{a.busy ? "Saving…" : "Update password"}</Button></form>}</Shell>;
}

export function Activate() {
  const { query } = useRoute(); const token = query.get("token") ?? ""; const a = useAction(); const [state, setState] = useState<"working" | "ok" | "fail">("working");
  useEffect(() => { a.run(() => account.activate(token)).then((r) => setState(r ? "ok" : "fail")); /* eslint-disable-next-line */ }, [token]);
  return <Shell title="Activating your account">{state === "working" && <Loading label="One moment…" />}{state === "ok" && <><Success message="Your account is active. You can now log in." /><Button variant="secondary" className="w-full" onClick={() => navigate("login")}>Log in</Button></>}{state === "fail" && <ErrorNote message={a.error || "This link is invalid or has expired."} />}</Shell>;
}

/** Public certificate verification: /#verify/CODE */
export function Verify() {
  const { param } = useRoute(); const [code, setCode] = useState(param ?? "");
  const [result, setResult] = useState<Verification | null>(null); const a = useAction();
  const check = async (c: string) => { if (c.trim()) setResult((await a.run(() => recognition.verify(c))) ?? null); };
  useEffect(() => { if (param) check(param); /* eslint-disable-next-line */ }, [param]);
  return <Shell title="Verify a certificate"><form className="space-y-3" onSubmit={(e) => { e.preventDefault(); navigate(`verify/${code.trim().toUpperCase()}`); }}><Field label="Verification code" placeholder="e.g. 7KQ2M9XTR4PB" value={code} onChange={setCode} /><Button type="submit" variant="secondary" className="w-full">{a.busy ? "Checking…" : "Verify"}</Button></form>
    <ErrorNote message={a.error} />
    {result && (result.valid ? <div className="rounded-2xl border border-[#efece6] bg-[#efece6] p-5" role="status"><p className="text-sm font-extrabold text-[#8a6d2f]">✓ Valid certificate</p><p className="mt-3 text-lg font-extrabold text-[#1a1a1a]">{result.holderName}</p><p className="text-sm text-slate-600">{result.activityTitle}</p><p className="mt-2 text-xs text-slate-500">No. {result.number} · Issued {fmtDate(result.issuedAt)} by {result.issuer}</p></div> :
      <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm font-semibold text-red-700">{result.status === "REVOKED" ? "This certificate has been revoked." : "No certificate matches that code. Check it and try again."}</div>)}
    <button onClick={() => navigate("home")} className="text-sm font-bold text-[#1a1a1a]">Back to YEP</button></Shell>;
}
