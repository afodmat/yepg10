import { ReactNode, Suspense, lazy } from "react";
import { navigate, useRoute } from "./lib/nav";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { PublicHeader, Footer } from "./components/Chrome";
import { DashboardLayout, useUnread } from "./layouts/DashboardLayout";
import { AuthPage } from "./pages/AuthPage";
import { Home } from "./pages/Home";
import { Learning } from "./pages/Learning";
import { Exchange } from "./pages/Exchange";
import { Projects } from "./pages/Projects";
import { Mentorship } from "./pages/Mentorship";
import { Dashboard } from "./pages/Dashboard";
const MyLearning = lazy(() => import("./pages/MyLearning").then((m) => ({ default: m.MyLearning })));
const TeamPage = lazy(() => import("./pages/Team").then((m) => ({ default: m.TeamPage })));
const Messages = lazy(() => import("./pages/Messages").then((m) => ({ default: m.Messages })));
import { Achievements } from "./pages/Achievements";
import { Notifications } from "./pages/Notifications";
const ProfilePage = lazy(() => import("./pages/ProfilePage").then((m) => ({ default: m.ProfilePage })));
const Mentees = lazy(() => import("./pages/Mentees").then((m) => ({ default: m.Mentees })));
const Manage = lazy(() => import("./pages/Manage").then((m) => ({ default: m.Manage })));
const Activate = lazy(() => import("./pages/AccountPages").then((m) => ({ default: m.Activate })));
const ForgotPassword = lazy(() => import("./pages/AccountPages").then((m) => ({ default: m.ForgotPassword })));
const ResetPassword = lazy(() => import("./pages/AccountPages").then((m) => ({ default: m.ResetPassword })));
const Verify = lazy(() => import("./pages/AccountPages").then((m) => ({ default: m.Verify })));

/** Pages that live inside the signed-in dashboard shell. */
const dashboardPages = new Set(["dashboard", "mylearning", "team", "messages", "achievements", "notifications", "profile"]);
const participantOnly = new Set(["mylearning", "team", "achievements"]);

function Shell() {
  const { user, loading, logout } = useAuth();
  const { page, query } = useRoute();
  const unread = useUnread(!!user);

  if (loading) return <div className="grid min-h-screen place-items-center text-sm font-semibold text-slate-500" role="status">Loading…</div>;
  const doLogout = async () => { await logout(); navigate("home"); };

  // standalone pages (no site chrome)
  if (page === "login" || page === "register") return <AuthPage register={page === "register"} />;
  if (page === "forgot") return <ForgotPassword />;
  if (page === "reset-password") return <ResetPassword />;
  if (page === "activate") return <Activate />;
  if (page === "verify") return <Verify />;

  if (dashboardPages.has(page)) {
    if (!user) return <AuthPage register={false} />;
    let content: ReactNode;
    if (participantOnly.has(page) && user.role !== "PARTICIPANT") content = <RoleHome role={user.role} />;
    else if (page === "dashboard") content = <RoleHome role={user.role} />;
    else if (page === "mylearning") content = <MyLearning />;
    else if (page === "team") content = <TeamPage />;
    else if (page === "messages") content = <Messages onRead={unread.refresh} />;
    else if (page === "achievements") content = <Achievements />;
    else if (page === "notifications") content = <Notifications onChange={unread.refresh} />;
    else content = <ProfilePage />;
    return <DashboardLayout page={page} onLogout={doLogout} unreadMessages={unread.messages} unreadNotices={unread.notices}>{content}</DashboardLayout>;
  }
  // mentors see their mentee list under this name too (linked from notifications)
  if (page === "mentees" && user?.role === "MENTOR") return <DashboardLayout page="dashboard" onLogout={doLogout} unreadMessages={unread.messages} unreadNotices={unread.notices}><Mentees /></DashboardLayout>;

  let content: ReactNode;
  if (page === "learning") content = <Learning />;
  else if (page === "exchange") content = <Exchange />;
  else if (page === "projects") content = <Projects />;
  else if (page === "mentorship") content = <Mentorship />;
  else content = <Home />;
  void query;
  return <><PublicHeader loggedIn={!!user} />{content}<Footer /></>;
}

function RoleHome({ role }: { role: string }) {
  if (role === "MENTOR") return <Mentees />;
  if (role === "FACILITATOR" || role === "ADMIN") return <Manage />;
  return <Dashboard />;
}

export default function App() { return <AuthProvider><Suspense fallback={<div className="grid min-h-[40vh] place-items-center text-sm font-semibold text-slate-400" role="status">Loading…</div>}><Shell /></Suspense></AuthProvider>; }
