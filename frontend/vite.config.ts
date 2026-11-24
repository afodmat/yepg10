import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: { alias: { "@": new URL("./src", import.meta.url).pathname } },
  test: { environment: "jsdom", setupFiles: ["./src/test-setup.ts"], globals: true },
  server: { port: 5173, proxy: { "/api": { target: process.env.VITE_PROXY_TARGET ?? "http://localhost:8080", changeOrigin: false } } },
});
