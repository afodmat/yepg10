import { http, qs } from "./client";
import type { Role, User } from "./auth";

export interface Page<T> { items: T[]; page: number; size: number; total: number; totalPages: number }

// ---------- learning ----------
export interface Activity { id: number; title: string; description: string; category: string; level: string; objectives?: string; facilitatorId?: number; facilitatorName?: string; startDate?: string; endDate?: string; durationHours?: number; capacity: number; enrolledCount: number; lessonsTotal: number; status: "DRAFT" | "OPEN" | "CLOSED" | "COMPLETED"; thumbnailUrl?: string }
export type ActivityInput = Omit<Activity, "id" | "facilitatorId" | "facilitatorName" | "enrolledCount">;
export interface Enrollment { id: number; activityId: number; title: string; category: string; level: string; status: "ENROLLED" | "COMPLETED" | "CANCELLED"; progressPct: number; completedLessons: number; lessonsTotal: number; finalScore?: number; enrolledAt: string; completedAt?: string; assessmentId?: number; assessmentPassed: boolean }
export interface Option { id: number; text: string; correct?: boolean }
export interface Question { id: number; text: string; points: number; options: Option[] }
export interface Assessment { id: number; activityId: number; title: string; passMark: number; maxAttempts: number; attemptsUsed: number; passed: boolean; questions: Question[] }
export interface AttemptResult { attemptId: number; scorePct: number; passed: boolean; correct: number; total: number; attemptsLeft: number; feedback: string }
export interface AssessmentInput { title: string; passMark: number; maxAttempts: number; questions: { text: string; points: number; options: { text: string; correct: boolean }[] }[] }

export const learning = {
  list: (p: { q?: string; category?: string; level?: string; page?: number; size?: number }) => http.get<Page<Activity>>(`/activities${qs(p)}`),
  get: (id: number) => http.get<Activity>(`/activities/${id}`),
  create: (a: ActivityInput) => http.post<Activity>("/activities", a),
  update: (id: number, a: ActivityInput) => http.put<Activity>(`/activities/${id}`, a),
  remove: (id: number) => http.del<void>(`/activities/${id}`),
  enroll: (id: number) => http.post<Enrollment>(`/activities/${id}/enroll`),
  cancel: (id: number) => http.del<void>(`/activities/${id}/enroll`),
  mine: () => http.get<Enrollment[]>("/enrollments/mine"),
  completeLesson: (id: number) => http.post<Enrollment>(`/activities/${id}/lessons/complete`),
  roster: (id: number, page = 0) => http.get<Page<{ userId: number; name: string; status: string; progressPct: number; finalScore?: number; enrolledAt: string }>>(`/activities/${id}/enrollments${qs({ page })}`),
  assessment: (activityId: number) => http.get<Assessment>(`/activities/${activityId}/assessment`),
  saveAssessment: (activityId: number, a: AssessmentInput) => http.put<Assessment>(`/activities/${activityId}/assessment`, a),
  submit: (assessmentId: number, answers: Record<number, number>) => http.post<AttemptResult>(`/assessments/${assessmentId}/submit`, { answers }),
  results: (assessmentId: number, page = 0) => http.get<Page<{ id: number; participant: string; scorePct: number; passed: boolean; submittedAt: string }>>(`/assessments/${assessmentId}/results${qs({ page })}`),
};

// ---------- exchange ----------
export interface Session { id: number; title: string; description: string; topic: string; startsAt: string; durationMinutes: number; facilitatorName?: string; status: string; phase: "UPCOMING" | "COMPLETED" | "CANCELLED"; participantCount: number; countries: string[]; registered: boolean; meetingLink?: string; discussionPrompts: string[]; resources: string[] }
export interface SessionInput { title: string; description: string; topic: string; startsAt: string; durationMinutes: number; meetingLink?: string; discussionPrompts?: string; resources?: string; status?: string }
export const exchange = {
  list: (p: { q?: string; when?: string; page?: number; size?: number }) => http.get<Page<Session>>(`/exchange-sessions${qs(p)}`),
  register: (id: number) => http.post<Session>(`/exchange-sessions/${id}/register`),
  unregister: (id: number) => http.del<void>(`/exchange-sessions/${id}/register`),
  create: (s: SessionInput) => http.post<Session>("/exchange-sessions", s),
  update: (id: number, s: SessionInput) => http.put<Session>(`/exchange-sessions/${id}`, s),
  remove: (id: number) => http.del<void>(`/exchange-sessions/${id}`),
};

// ---------- teams & projects ----------
export interface Member { userId: number; name: string; country?: string; role: "LEADER" | "MEMBER"; title?: string; joinedAt: string }
export interface Team { id: number; name: string; description?: string; leaderId: number; leaderName: string; country?: string; institution?: string; category?: string; open: boolean; status: string; memberCount: number; member: boolean; createdAt: string; members?: Member[]; project?: { id: number; name: string; progressPct: number; status: string }; mentors?: string[] }
export interface TeamInput { name: string; description?: string; country?: string; institution?: string; category?: string; open: boolean }
export interface Invitation { id: number; teamId: number; teamName: string; invitedBy: string; createdAt: string }
export interface Milestone { id: number; name: string; status: "PENDING" | "IN_PROGRESS" | "DONE"; completedAt?: string }
export interface Post { id: number; authorId: number; author: string; content: string; createdAt: string }
export interface Project { id: number; teamId: number; teamName: string; name: string; description: string; problemStatement?: string; solution?: string; category: string; location?: string; leaderId: number; leaderName: string; status: string; progressPct: number; startDate?: string; targetDate?: string; mentors: string[]; canView: boolean; canEdit: boolean; canContribute: boolean; canReview: boolean; milestones: Milestone[]; updates: Post[]; comments: Post[]; attachments: { fileId: number; name: string; contentType: string }[] }
export interface ProjectInput { teamId?: number; name: string; description: string; problemStatement?: string; solution?: string; category: string; location?: string; startDate?: string; targetDate?: string }

export const teams = {
  list: (p: { q?: string; mine?: boolean; page?: number; size?: number }) => http.get<Page<Team>>(`/teams${qs(p)}`),
  get: (id: number) => http.get<Team>(`/teams/${id}`),
  create: (t: TeamInput) => http.post<Team>("/teams", t),
  update: (id: number, t: TeamInput, status?: string) => http.put<Team>(`/teams/${id}${qs({ status })}`, t),
  join: (id: number) => http.post<Team>(`/teams/${id}/join`),
  leave: (id: number) => http.del<void>(`/teams/${id}/members/me`),
  remove: (id: number, userId: number) => http.del<void>(`/teams/${id}/members/${userId}`),
  setTitle: (id: number, userId: number, title: string) => http.put<void>(`/teams/${id}/members/${userId}/title`, { title }),
  invite: (id: number, email: string) => http.post<{ message: string }>(`/teams/${id}/invitations`, { email }),
  invitations: () => http.get<Invitation[]>("/invitations/mine"),
  accept: (id: number) => http.post<Team>(`/invitations/${id}/accept`),
  decline: (id: number) => http.post<void>(`/invitations/${id}/decline`),
};
export const projects = {
  list: (p: { q?: string; category?: string; mine?: boolean; page?: number; size?: number }) => http.get<Page<Project>>(`/projects${qs(p)}`),
  get: (id: number) => http.get<Project>(`/projects/${id}`),
  create: (p: ProjectInput) => http.post<Project>("/projects", p),
  update: (id: number, p: ProjectInput) => http.put<Project>(`/projects/${id}`, p),
  setStatus: (id: number, status: string) => http.patch<Project>(`/projects/${id}/status`, { status }),
  postUpdate: (id: number, content: string, milestoneId?: number, milestoneStatus?: string) => http.post<Project>(`/projects/${id}/updates`, { content, milestoneId, milestoneStatus }),
  milestone: (id: number, mid: number, status: string) => http.patch<Project>(`/projects/${id}/milestones/${mid}`, { status }),
  comment: (id: number, content: string) => http.post<Project>(`/projects/${id}/comments`, { content }),
  attach: (id: number, fileId: number) => http.post<Project>(`/projects/${id}/attachments`, { fileId }),
};

// ---------- mentorship & messaging ----------
export interface Mentor { userId: number; name: string; country?: string; expertise: string; availability?: string; bio?: string; activeMentees: number; maxMentees: number; accepting: boolean }
export interface MentorRequest { id: number; participantId: number; participant: string; participantCountry?: string; mentorId: number; mentor: string; message?: string; status: string; createdAt: string }
export interface Assignment { id: number; type: "PARTICIPANT" | "TEAM"; mentorId: number; mentor: string; participantId?: number; teamId?: number; mentee: string; country?: string; status: string; learningProgress?: number; projectProgress?: number; createdAt: string }
export const mentorship = {
  mentors: (p: { q?: string; page?: number }) => http.get<Page<Mentor>>(`/mentors${qs(p)}`),
  myProfile: () => http.get<{ expertise: string; availability?: string; bio?: string; maxMentees: number } | null>("/mentors/me"),
  saveProfile: (b: { expertise: string; availability?: string; bio?: string; maxMentees: number }) => http.put<void>("/mentors/me", b),
  request: (mentorId: number, message: string) => http.post<void>("/mentorship/requests", { mentorId, message }),
  sent: () => http.get<MentorRequest[]>("/mentorship/requests/sent"),
  incoming: () => http.get<MentorRequest[]>("/mentorship/requests/incoming"),
  respond: (id: number, accept: boolean) => http.post<void>(`/mentorship/requests/${id}/respond`, { accept }),
  mine: () => http.get<Assignment[]>("/mentorship/mine"),
  all: (page = 0) => http.get<Page<Assignment>>(`/mentorship/assignments${qs({ page })}`),
  assign: (b: { mentorId: number; participantId?: number; teamId?: number }) => http.post<void>("/mentorship/assignments", b),
  end: (id: number) => http.del<void>(`/mentorship/assignments/${id}`),
};
export interface Conversation { id: number; type: "DIRECT" | "TEAM"; title: string; subtitle: string; lastMessage?: string; lastAt?: string; unread: number }
export interface Message { id: number; senderId: number; sender: string; body: string; createdAt: string; mine: boolean }
export const messaging = {
  list: () => http.get<Conversation[]>("/conversations"),
  unread: () => http.get<{ count: number }>("/conversations/unread-count"),
  direct: (userId: number) => http.post<Conversation>("/conversations/direct", { userId }),
  messages: (id: number, after?: number) => http.get<Message[]>(`/conversations/${id}/messages${qs({ after })}`),
  send: (id: number, body: string) => http.post<Message>(`/conversations/${id}/messages`, { body }),
  read: (id: number) => http.post<void>(`/conversations/${id}/read`),
};

// ---------- recognition, notifications, dashboard ----------
export interface Achievement { id: number; name: string; description: string; icon: string; criteria: string; earned: boolean; awardedAt?: string }
export interface Certificate { id: number; number: string; activityTitle: string; holderName: string; issuer: string; issuedAt: string; verificationCode: string; status: string }
export interface Verification { valid: boolean; status: string; holderName?: string; activityTitle?: string; number?: string; issuer?: string; issuedAt?: string }
export interface Notice { id: number; type: string; title: string; body?: string; link?: string; read: boolean; createdAt: string }
export const recognition = {
  achievements: () => http.get<Achievement[]>("/achievements"),
  certificates: () => http.get<Certificate[]>("/certificates"),
  verify: (code: string) => http.get<Verification>(`/certificates/verify/${encodeURIComponent(code)}`),
};
export const notices = {
  list: (page = 0) => http.get<Page<Notice>>(`/notifications${qs({ page })}`),
  unread: () => http.get<{ count: number }>("/notifications/unread-count"),
  read: (id: number) => http.post<void>(`/notifications/${id}/read`),
  readAll: () => http.post<void>("/notifications/read-all"),
  prefs: () => http.get<{ type: string; enabled: boolean }[]>("/notifications/preferences"),
  setPref: (type: string, enabled: boolean) => http.put<void>("/notifications/preferences", { type, enabled }),
};
export interface Dashboard { learning: Enrollment[]; upcomingSessions: Session[]; team?: Team; recentMessages: Conversation[]; unreadMessages: number; unreadNotifications: number; achievements: Achievement[]; achievementsEarned: number; achievementsTotal: number; certificates: number; mentors: Assignment[]; pendingInvitations: number }
export const dashboard = {
  get: () => http.get<Dashboard>("/dashboard"),
  publicStats: () => http.get<{ participants: number; countries: number; activities: number; projects: number }>("/public/stats"),
};

// ---------- profile, files, admin ----------
export interface Profile { firstName: string; lastName: string; email: string; phone?: string; country: string; age?: number; bio?: string; skills?: string; interests?: string; education?: string; organization?: string; role: Role; status: string; photoFileId?: number }
export type ProfileInput = Omit<Profile, "email" | "role" | "status" | "photoFileId">;
export const account = {
  profile: () => http.get<Profile>("/users/me/profile"),
  save: (p: ProfileInput) => http.put<Profile>("/users/me", p),
  changePassword: (currentPassword: string, newPassword: string) => http.put<void>("/users/me/password", { currentPassword, newPassword }),
  setPhoto: (fileId: number) => http.put<void>("/users/me/photo", { fileId }),
  upload: (file: File, purpose: string) => { const f = new FormData(); f.append("file", file); f.append("purpose", purpose); return http.upload<{ id: number; name: string }>("/files", f); },
  forgot: (email: string) => http.post<{ message: string }>("/auth/forgot-password", { email }),
  reset: (token: string, newPassword: string) => http.post<{ message: string }>("/auth/reset-password", { token, newPassword }),
  activate: (token: string) => http.get<{ message: string }>(`/auth/activate${qs({ token })}`),
};
export const fileUrl = (id: number) => `${import.meta.env.VITE_API_URL ?? "/api"}/files/${id}`;

export interface AdminUser { id: number; email: string; firstName: string; lastName: string; role: Role; status: string; country: string; createdAt: string; lastLoginAt?: string }
export interface AuditRow { id: number; actorEmail?: string; action: string; entityType?: string; entityId?: string; metadata?: Record<string, unknown>; createdAt: string }
export const admin = {
  stats: () => http.get<Record<string, number>>("/admin/stats"),
  users: (p: { q?: string; role?: string; status?: string; sort?: string; dir?: string; page?: number; size?: number }) => http.get<Page<AdminUser>>(`/admin/users${qs(p)}`),
  createUser: (b: { firstName: string; lastName: string; email: string; country: string; role: Role; temporaryPassword: string }) => http.post<AdminUser>("/admin/users", b),
  setRole: (id: number, role: Role) => http.patch<void>(`/admin/users/${id}/role`, { role }),
  setStatus: (id: number, status: string) => http.patch<void>(`/admin/users/${id}/status`, { status }),
  audit: (p: { action?: string; page?: number }) => http.get<Page<AuditRow>>(`/admin/audit-logs${qs(p)}`),
  settings: () => http.get<Record<string, string>>("/admin/settings"),
  saveSettings: (s: Record<string, string>) => http.put<void>("/admin/settings", s),
  broadcast: (b: { title: string; body?: string; role?: Role }) => http.post<{ recipients: number }>("/admin/notifications", b),
  achievements: () => http.get<{ id: number; code: string; name: string; description: string; icon: string; criteria: string; active: boolean }[]>("/admin/achievements"),
  saveAchievement: (id: number | null, b: { code: string; name: string; description: string; icon: string; criteria: string; active: boolean }) => id ? http.put<unknown>(`/admin/achievements/${id}`, b) : http.post<unknown>("/admin/achievements", b),
  certificates: (page = 0) => http.get<Page<Certificate>>(`/admin/certificates${qs({ page })}`),
  revokeCertificate: (id: number) => http.post<void>(`/admin/certificates/${id}/revoke`),
};
export type { User, Role };

export const directory = { participants: (q: string) => http.get<{ id: number; name: string; email: string }[]>(`/directory/participants${qs({ q })}`) };
