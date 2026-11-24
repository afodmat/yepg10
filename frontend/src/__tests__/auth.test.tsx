import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "../context/AuthContext";
import { AuthPage } from "../pages/AuthPage";

const json = (status: number, body: unknown) => Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
let calls: { url: string; init?: RequestInit }[] = [];

beforeEach(() => {
  calls = [];
  location.hash = "#login";
  vi.stubGlobal("fetch", vi.fn((url: string, init?: RequestInit) => {
    calls.push({ url, init });
    if (url.endsWith("/users/me")) return json(401, { message: "Please log in" });
    if (url.endsWith("/auth/login")) return json(401, { message: "Invalid email or password." });
    if (url.endsWith("/auth/register")) return json(201, { message: "Almost there! Check your email." });
    return json(404, {});
  }));
});
afterEach(() => vi.unstubAllGlobals());

const renderAuth = (register: boolean) => render(<AuthProvider><AuthPage register={register} /></AuthProvider>);

describe("login", () => {
  it("shows the server's generic error on invalid credentials and never stores credentials locally", async () => {
    renderAuth(false);
    await userEvent.type(await screen.findByLabelText(/email address/i), "amina@example.com");
    await userEvent.type(screen.getByLabelText(/^password/i), "WrongPass1");
    await userEvent.click(screen.getByRole("button", { name: /log in securely/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid email or password.");
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    const login = calls.find((c) => c.url.endsWith("/auth/login"))!;
    expect(login.init?.credentials).toBe("include");   // cookie-based session, no token handling in JS
  });
});

describe("registration", () => {
  const fill = async (password: string, confirm: string) => {
    await userEvent.type(screen.getByLabelText(/first name/i), "Amina");
    await userEvent.type(screen.getByLabelText(/last name/i), "Kato");
    await userEvent.type(screen.getByLabelText(/email address/i), "amina@example.com");
    await userEvent.type(screen.getByLabelText(/phone number/i), "+256700000000");
    await userEvent.type(screen.getByLabelText(/^age/i), "24");
    await userEvent.type(screen.getByLabelText(/^password/i), password);
    await userEvent.type(screen.getByLabelText(/confirm password/i), confirm);
    await userEvent.click(screen.getByRole("checkbox"));
    await userEvent.click(screen.getByRole("button", { name: /create my account/i }));
  };

  it("blocks submission when passwords differ", async () => {
    renderAuth(true);
    await fill("Passw0rdOK", "Different1");
    expect(await screen.findByRole("alert")).toHaveTextContent(/do not match/i);
    expect(calls.some((c) => c.url.endsWith("/auth/register"))).toBe(false);
  });

  it("sends the form to the API without any role field and shows the activation notice", async () => {
    renderAuth(true);
    await fill("Passw0rdOK", "Passw0rdOK");
    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent(/check your email/i));
    const body = JSON.parse(calls.find((c) => c.url.endsWith("/auth/register"))!.init!.body as string);
    expect(body).toMatchObject({ firstName: "Amina", email: "amina@example.com", age: 24 });
    expect(body).not.toHaveProperty("role");
  });
});
