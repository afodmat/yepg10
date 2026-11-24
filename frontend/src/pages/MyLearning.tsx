import { useState } from "react";
import { Icon } from "../components/Icon";
import { Button } from "../components/ui";
import { Badge, Card, Confirm, Empty, ErrorNote, Loading, Modal, PageTitle, ProgressBar, statusTone } from "../components/common";
import { learning, type Assessment, type AttemptResult, type Enrollment } from "../api/endpoints";
import { useAction, useApi } from "../hooks/useApi";
import { categoryIcon, fmtDate, tone } from "../lib/format";
import { navigate } from "../lib/nav";

function AssessmentModal({ activityId, onClose, onDone }: { activityId: number; onClose: () => void; onDone: () => void }) {
  const a = useApi(() => learning.assessment(activityId), [activityId]);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [result, setResult] = useState<AttemptResult | null>(null);
  const action = useAction();
  const quiz: Assessment | null = a.data;
  const submit = async () => { if (!quiz) return; const r = await action.run(() => learning.submit(quiz.id, answers)); if (r) { setResult(r); onDone(); } };
  return <Modal title={quiz?.title ?? "Assessment"} onClose={onClose} wide>
    {a.loading && <Loading />}<ErrorNote message={a.error} />
    {result ? <div className="text-center"><p className={`text-5xl font-extrabold ${result.passed ? "text-[#8a6d2f]" : "text-[#8a6d2f]"}`}>{Math.round(result.scorePct)}%</p><p className="mt-2 font-bold">{result.correct} of {result.total} correct</p><p className="mt-3 text-sm text-slate-600">{result.feedback}</p><Button onClick={onClose} className="mt-6">Close</Button></div> :
      quiz && <><p className="text-sm text-slate-500">Pass mark {quiz.passMark}% · attempt {quiz.attemptsUsed + 1} of {quiz.maxAttempts}</p>
        <div className="mt-5 space-y-6">{quiz.questions.map((q, i) => <fieldset key={q.id}><legend className="font-bold text-slate-800">{i + 1}. {q.text}</legend><div className="mt-2 space-y-2">{q.options.map((o) => <label key={o.id} className={`flex cursor-pointer items-center gap-3 rounded-xl border p-3 text-sm ${answers[q.id] === o.id ? "border-[#1a1a1a] bg-[#efece6]" : "border-slate-200"}`}><input type="radio" name={`q${q.id}`} checked={answers[q.id] === o.id} onChange={() => setAnswers({ ...answers, [q.id]: o.id })} />{o.text}</label>)}</div></fieldset>)}</div>
        <div className="mt-4"><ErrorNote message={action.error} /></div>
        <div className="mt-6 flex justify-end"><Button onClick={submit}>{action.busy ? "Submitting…" : "Submit answers"}</Button></div></>}
  </Modal>;
}

export function MyLearning() {
  const list = useApi(() => learning.mine());
  const action = useAction();
  const [quiz, setQuiz] = useState<number | null>(null);
  const [cancel, setCancel] = useState<Enrollment | null>(null);
  const lesson = async (e: Enrollment) => { await action.run(() => learning.completeLesson(e.activityId)); list.reload(); };

  return <main className="p-5 lg:p-9">
    <PageTitle eyebrow="My learning" title="Your learning activities" sub="Complete lessons, pass the assessment and earn your certificate." action={<Button variant="ghost" onClick={() => navigate("learning")}>Find more <Icon name="arrow" size={16} /></Button>} />
    <div className="mt-6 space-y-3"><ErrorNote message={action.error || list.error} onRetry={list.reload} /></div>
    {list.loading && !list.data ? <Loading /> : list.data?.length === 0 ? <div className="mt-6"><Empty title="You haven't enrolled in anything yet" hint="Pick an activity to get started." action={<Button variant="secondary" onClick={() => navigate("learning")}>Browse learning</Button>} /></div> :
      <div className="mt-6 space-y-4">{list.data?.map((e) => {
        const lessonsDone = e.completedLessons >= e.lessonsTotal;
        return <Card key={e.id}><div className="flex flex-col gap-5 sm:flex-row sm:items-center">
          <span className={`grid h-14 w-14 shrink-0 place-items-center rounded-2xl ${tone(e.category)}`}><Icon name={categoryIcon(e.category)} size={24} /></span>
          <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="text-lg font-extrabold text-[#1a1a1a]">{e.title}</h2><Badge tone={statusTone(e.status)}>{e.status.toLowerCase()}</Badge></div>
            <p className="mt-1 text-xs text-slate-500">{e.category} · {e.level} · enrolled {fmtDate(e.enrolledAt)}{e.completedAt ? ` · completed ${fmtDate(e.completedAt)}` : ""}</p>
            <div className="mt-3 flex items-center gap-3"><div className="flex-1"><ProgressBar value={e.progressPct} height="h-2.5" /></div><span className="w-10 text-right text-sm font-extrabold">{e.progressPct}%</span></div>
            <p className="mt-2 text-xs text-slate-500">{e.completedLessons} of {e.lessonsTotal} lessons{e.assessmentId ? ` · assessment ${e.assessmentPassed ? `passed${e.finalScore != null ? ` (${Math.round(e.finalScore)}%)` : ""}` : "not passed yet"}` : ""}</p></div>
          <div className="flex flex-wrap gap-2 sm:flex-col">
            {e.status === "ENROLLED" && <>
              <Button variant="secondary" onClick={() => lesson(e)} className={lessonsDone ? "pointer-events-none opacity-40" : ""}>{lessonsDone ? "Lessons done" : "Complete next lesson"}</Button>
              {e.assessmentId && !e.assessmentPassed && <Button variant="primary" onClick={() => setQuiz(e.activityId)}>Take assessment</Button>}
              <button onClick={() => setCancel(e)} className="text-xs font-bold text-slate-400 hover:text-red-600">Cancel enrollment</button></>}
            {e.status === "COMPLETED" && <Button variant="primary" onClick={() => navigate("achievements")}>View certificate</Button>}
          </div></div></Card>;
      })}</div>}
    {quiz !== null && <AssessmentModal activityId={quiz} onClose={() => setQuiz(null)} onDone={list.reload} />}
    {cancel && <Confirm danger title="Cancel enrollment?" message={`You will lose your progress in "${cancel.title}". You can enroll again later.`} confirmLabel="Cancel enrollment" onClose={() => setCancel(null)} onConfirm={async () => { await action.run(() => learning.cancel(cancel.activityId)); list.reload(); }} />}
  </main>;
}
