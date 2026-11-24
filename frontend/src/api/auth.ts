import { api } from "./client";

export type Role = "PARTICIPANT" | "MENTOR" | "FACILITATOR" | "ADMIN";
export interface User {
  id: number; email: string; firstName: string; lastName: string; role: Role; country?: string; status: string; photoFileId?: number;
}
export interface RegisterRequest {
  firstName: string; lastName: string; email: string; phone: string; country: string; age: number; password: string;
}

export const authApi = {
  register: (r: RegisterRequest) => api<{ message: string }>("/auth/register", { method: "POST", body: r }),
  login: (email: string, password: string) => api<User>("/auth/login", { method: "POST", body: { email, password } }),
  logout: () => api<void>("/auth/logout", { method: "POST" }),
  me: () => api<User>("/users/me"),
};
