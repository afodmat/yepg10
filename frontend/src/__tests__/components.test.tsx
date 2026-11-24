import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Confirm, Pagination, ProgressBar } from "../components/common";
import { PasswordInput } from "../components/ui";

describe("shared components", () => {
  it("exposes progress to assistive tech and clamps the value", () => {
    render(<ProgressBar value={140} />);
    expect(screen.getByRole("progressbar")).toHaveAttribute("aria-valuenow", "140");
    expect(screen.getByRole("progressbar").firstElementChild).toHaveStyle({ width: "100%" });
  });

  it("asks for confirmation before a destructive action runs", async () => {
    const onConfirm = vi.fn(); const onClose = vi.fn();
    render(<Confirm danger title="Delete?" message="Gone forever" confirmLabel="Delete" onConfirm={onConfirm} onClose={onClose} />);
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onConfirm).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole("button", { name: "Delete" }));
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it("paginates with disabled bounds", async () => {
    const onPage = vi.fn();
    render(<Pagination page={0} totalPages={3} total={25} onPage={onPage} />);
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(onPage).toHaveBeenCalledWith(1);
  });

  it("shows and hides password input text on request", async () => {
    render(<PasswordInput label="New password" value="Secret123" onChange={() => {}} />);
    const input = screen.getByLabelText("New password");
    expect(input).toHaveAttribute("type", "password");
    await userEvent.click(screen.getByRole("button", { name: "Show password" }));
    expect(input).toHaveAttribute("type", "text");
    await userEvent.click(screen.getByRole("button", { name: "Hide password" }));
    expect(input).toHaveAttribute("type", "password");
  });
});
