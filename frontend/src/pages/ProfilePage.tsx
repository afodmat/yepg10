import { FormEvent, useEffect, useState } from "react";
import { Button, PasswordInput } from "../components/ui";
import { Avatar, Card, ErrorNote, Labeled, Loading, PageTitle, Success, areaCls, inputCls } from "../components/common";
import { account, fileUrl, mentorship, notices, type ProfileInput } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";
import { useAction, useApi } from "../hooks/useApi";

const typeLabel: Record<string, string> = { ENROLLMENT: "Enrollments", LEARNING_ACTIVITY: "Learning and exchange", TEAM_INVITATION: "Team invitations", PROJECT_UPDATE: "Project updates", MENTOR_MESSAGE: "Mentor messages", ASSESSMENT_RESULT: "Assessment results", ACHIEVEMENT: "Achievements", CERTIFICATE: "Certificates" };

export function ProfilePage() {
  const { user, refresh } = useAuth();
  const profile = useApi(() => account.profile());
  const [f, setF] = useState<ProfileInput | null>(null);
  const [photo, setPhoto] = useState<number | undefined>();
  const save = useAction(); const pwd = useAction(); const pic = useAction(); const mentorAct = useAction();
  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  const prefs = useApi(() => notices.prefs());
  const mentor = useApi(async () => user?.role === "MENTOR" ? mentorship.myProfile() : null, [user?.role]);
  const [m, setM] = useState({ expertise: "", availability: "", bio: "", maxMentees: 10 });

  useEffect(() => { if (profile.data) { const { email, role, status, photoFileId, ...rest } = profile.data; setF(rest); setPhoto(photoFileId); } }, [profile.data]);
  useEffect(() => { if (mentor.data) setM({ expertise: mentor.data.expertise, availability: mentor.data.availability ?? "", bio: mentor.data.bio ?? "", maxMentees: mentor.data.maxMentees }); }, [mentor.data]);

  if (!f) return <main className="p-5 lg:p-9">{profile.loading ? <Loading /> : <ErrorNote message={profile.error} onRetry={profile.reload} />}</main>;
  const set = (k: keyof ProfileInput) => (v: string) => setF({ ...f, [k]: k === "age" ? (v ? Number(v) : undefined) : v } as ProfileInput);
  const txt = (k: keyof ProfileInput, label: string, extra: object = {}) => <Labeled label={label}><input className={inputCls} value={(f[k] as string | number | undefined) ?? ""} onChange={(e) => set(k)(e.target.value)} {...extra} /></Labeled>;

  const submit = async (e: FormEvent) => { e.preventDefault(); if (await save.run(() => account.save(f), "Profile saved.")) { profile.reload(); refresh(); } };
  const changePw = async (e: FormEvent) => { e.preventDefault(); if (pw.next !== pw.confirm) { pwd.run(async () => { throw new Error(); }); return; } if (await pwd.run(() => account.changePassword(pw.current, pw.next), "Password changed. Other devices were signed out.")) setPw({ current: "", next: "", confirm: "" }); };

  return <main className="p-5 lg:p-9"><PageTitle eyebrow="Account" title="Your profile" sub={`${profile.data?.email} · ${profile.data?.role.toLowerCase()}`} />
    <div className="mt-6 grid gap-6 xl:grid-cols-[1.4fr_.6fr]">
      <Card><form onSubmit={submit} className="space-y-4">
        <div className="flex items-center gap-4">{photo ? <img src={fileUrl(photo)} alt="Your profile" loading="lazy" className="h-16 w-16 rounded-full object-cover" /> : <Avatar name={`${f.firstName} ${f.lastName}`} size="h-16 w-16" />}
          <label className="inline-flex min-h-10 cursor-pointer items-center rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-600">Change photo<input type="file" accept=".jpg,.jpeg,.png" className="sr-only" onChange={async (e) => { const file = e.target.files?.[0]; if (!file) return; const up = await pic.run(() => account.upload(file, "PROFILE_PHOTO")); if (up && (await pic.run(() => account.setPhoto(up.id), "Photo updated."))) { setPhoto(up.id); refresh(); } }} /></label></div>
        <ErrorNote message={pic.error} /><Success message={pic.ok} />
        <div className="grid gap-4 sm:grid-cols-2">{txt("firstName", "First name", { required: true })}{txt("lastName", "Last name", { required: true })}{txt("phone", "Phone", { type: "tel" })}{txt("country", "Country", { required: true })}{txt("age", "Age", { type: "number", min: 16, max: 120 })}{txt("education", "Education")}</div>
        {txt("organization", "Organisation or institution")}
        <Labeled label="About you"><textarea maxLength={1000} className={areaCls} value={f.bio ?? ""} onChange={(e) => set("bio")(e.target.value)} /></Labeled>
        <div className="grid gap-4 sm:grid-cols-2">{txt("skills", "Skills")}{txt("interests", "Interests")}</div>
        <ErrorNote message={save.error} /><Success message={save.ok} /><Button type="submit" variant="secondary">{save.busy ? "Saving…" : "Save profile"}</Button></form></Card>
      <div className="space-y-6">
        <Card><h2 className="text-lg font-extrabold">Change password</h2><form onSubmit={changePw} className="mt-4 space-y-3">
          <PasswordInput label="Current password" autoComplete="current-password" className={inputCls} value={pw.current} onChange={(current) => setPw({ ...pw, current })} />
          <PasswordInput label="New password" hint="8+ characters with letters and a number" minLength={8} autoComplete="new-password" className={inputCls} value={pw.next} onChange={(next) => setPw({ ...pw, next })} />
          <PasswordInput label="Confirm new password" autoComplete="new-password" className={inputCls} value={pw.confirm} onChange={(confirm) => setPw({ ...pw, confirm })} />
          {pw.confirm && pw.next !== pw.confirm && <p role="alert" className="text-sm text-red-600">Passwords do not match.</p>}<ErrorNote message={pwd.error && pw.next !== pw.confirm ? "" : pwd.error} /><Success message={pwd.ok} /><Button type="submit" variant="secondary">Update password</Button></form></Card>
        <Card><h2 className="text-lg font-extrabold">Notification preferences</h2><div className="mt-4 space-y-3">{prefs.data?.map((p) => <label key={p.type} className="flex items-center justify-between text-sm font-semibold text-slate-600">{typeLabel[p.type] ?? p.type}<input type="checkbox" checked={p.enabled} onChange={async (e) => { await notices.setPref(p.type, e.target.checked); prefs.reload(); }} className="h-4 w-4" /></label>)}</div></Card>
      </div></div>
    {user?.role === "MENTOR" && <Card className="mt-6"><h2 className="text-lg font-extrabold">Mentor profile</h2><p className="text-sm text-slate-500">This is what participants see in the mentor directory.</p>
      <form className="mt-4 grid gap-4 sm:grid-cols-2" onSubmit={async (e) => { e.preventDefault(); await mentorAct.run(() => mentorship.saveProfile(m), "Mentor profile saved."); }}>
        <Labeled label="Expertise"><input required className={inputCls} value={m.expertise} onChange={(e) => setM({ ...m, expertise: e.target.value })} /></Labeled>
        <Labeled label="Availability"><input className={inputCls} value={m.availability} onChange={(e) => setM({ ...m, availability: e.target.value })} /></Labeled>
        <Labeled label="Bio"><textarea className={areaCls} value={m.bio} onChange={(e) => setM({ ...m, bio: e.target.value })} /></Labeled>
        <Labeled label="Maximum mentees"><input type="number" min={1} max={100} className={inputCls} value={m.maxMentees} onChange={(e) => setM({ ...m, maxMentees: Number(e.target.value) })} /></Labeled>
        <div className="sm:col-span-2"><ErrorNote message={mentorAct.error} /><Success message={mentorAct.ok} /><Button type="submit" variant="secondary">Save mentor profile</Button></div></form></Card>}
  </main>;
}
