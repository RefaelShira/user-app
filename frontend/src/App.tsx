import React, { useEffect, useMemo, useState } from "react";

// ---------- Types ----------
type Envelope<T> = {
  success: boolean;
  data?: T;
  error?: { code: string; message: string; details?: unknown };
};

type User = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  active: boolean;
};

type PageMeta = { totalElements: number; page: number; size: number };

type PagedResponse<T> = { items: T[]; meta: PageMeta };

type UserStats = { createdLast24h: number };

// Prefer env var if provided (vite): create .env.local with VITE_API_BASE=http://localhost:9090
const API_BASE = (import.meta as any).env?.VITE_API_BASE ?? "";

// ---------- Fetch helpers (unwrap {code,status,data}) ----------
async function apiFetch<T>(input: RequestInfo, init?: RequestInit): Promise<Envelope<T>> {
  const res = await fetch(input, init);

  let json: any = null;
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) {
    try {
      json = await res.json();
    } catch {
      // allow empty body (e.g., 201 without JSON)
    }
  }

  if (!res.ok) {
    const msg = json?.error?.message ?? json?.message ?? `${res.status} ${res.statusText}`;
    throw new Error(msg);
  }

  // Server envelope: { code, status, data, error }
  if (json && typeof json.success !== "boolean" && typeof json?.code === "number" && "data" in json) {
    const ok = json.code >= 200 && json.code < 300;
    const err = json.error
      ? { code: String(json.error.code ?? json.code), message: json.error.message ?? json.status ?? "Error" }
      : undefined;
    return { success: ok, data: json.data as T, error: err };
  }

  // Our envelope already
  if (json && typeof json.success === "boolean") {
    return json as Envelope<T>;
  }

  // Fallback
  return { success: true, data: json as T };
}

function apiGet<T>(path: string) {
  return apiFetch<T>(`${API_BASE}${path}`);
}

function apiPost<T>(path: string, body: any) {
  return apiFetch<T>(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

function apiDelete<T>(path: string) {
  return apiFetch<T>(`${API_BASE}${path}`, { method: "DELETE" });
}

// ---------- UI PRIMITIVES (Tailwind) ----------
type BadgeProps = React.PropsWithChildren<{ tone?: "slate" | "green" | "blue" | "red" }>;
function Badge({ children, tone = "slate" }: BadgeProps) {
  const tones: Record<string, string> = {
    slate: "bg-slate-100 text-slate-700 border-slate-200",
    green: "bg-green-100 text-green-700 border-green-200",
    blue: "bg-blue-100 text-blue-700 border-blue-200",
    red: "bg-rose-100 text-rose-700 border-rose-200",
  };
  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${tones[tone]}`}>
      {children}
    </span>
  );
}

type ButtonProps = React.PropsWithChildren<
  React.ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: "primary" | "ghost" | "danger" | "secondary";
  }
>;
function Button({ className = "", variant = "primary", ...rest }: ButtonProps) {
  const base =
    "inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold shadow-sm transition focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:opacity-50";
  const styles: Record<string, string> = {
    primary: "bg-slate-900 text-white hover:bg-slate-800 focus-visible:ring-slate-400",
    secondary: "bg-white text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50 focus-visible:ring-slate-300",
    ghost: "bg-white/60 text-slate-900 ring-1 ring-slate-200 hover:bg-white focus-visible:ring-slate-300",
    danger: "bg-rose-600 text-white hover:bg-rose-500 focus-visible:ring-rose-400",
  };
  return <button className={`${base} ${styles[variant]} ${className}`} {...rest} />;
}

function Card({ children, className = "" }: React.PropsWithChildren<{ className?: string }>) {
  return (
    <div className={`rounded-3xl border border-slate-200/80 bg-white/80 p-5 shadow-sm backdrop-blur ${className}`}>
      {children}
    </div>
  );
}

function SectionTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-2 flex items-end justify-between">
      <h2 className="text-lg font-semibold text-slate-800">{title}</h2>
      {subtitle && <p className="text-xs text-slate-500">{subtitle}</p>}
    </div>
  );
}

function Input(
  props: React.InputHTMLAttributes<HTMLInputElement> & { leftIcon?: React.ReactNode; className?: string }
) {
  const { leftIcon, className = "", ...rest } = props;
  return (
    <div className={`flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 shadow-sm focus-within:ring-2 focus-within:ring-slate-300 ${className}`}>
      {leftIcon}
      <input className="w-full bg-transparent outline-none placeholder:text-slate-400" {...rest} />
    </div>
  );
}

function Select(props: React.SelectHTMLAttributes<HTMLSelectElement> & { className?: string }) {
  const { className = "", ...rest } = props;
  return (
    <select
      className={`rounded-xl border border-slate-200 bg-white px-3 py-2 shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-300 ${className}`}
      {...rest}
    />
  );
}

function Checkbox({ checked, onChange, label }: { checked: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700">
      <input
        type="checkbox"
        className="h-4 w-4 rounded border-slate-300 text-slate-900 focus:ring-slate-400"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
      {label}
    </label>
  );
}

function Spinner() {
  return <div className="inline-block h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-slate-900" />;
}

function Modal({ open, onClose, title, children }: React.PropsWithChildren<{ open: boolean; onClose: () => void; title: string }>) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold text-slate-800">{title}</h3>
          <button className="text-slate-400 hover:text-slate-600" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function Toast({ message, tone = "green" as "green" | "red", onClose }: { message: string; tone?: "green" | "red"; onClose: () => void }) {
  const tones: Record<string, string> = {
    green: "bg-emerald-600",
    red: "bg-rose-600",
  };
  return (
    <div className={`fixed bottom-4 right-4 z-50 rounded-xl px-4 py-3 text-sm text-white shadow-lg ${tones[tone]}`}>
      <div className="flex items-center gap-3">
        <span> {message} </span>
        <button className="opacity-80 hover:opacity-100" onClick={onClose}>
          ✕
        </button>
      </div>
    </div>
  );
}

export default function App() {
  // --- Create form state ---
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [creating, setCreating] = useState(false);

  // Toast state
  const [toast, setToast] = useState<{ msg: string; tone?: "green" | "red" } | null>(null);

  // --- List state ---
  const [q, setQ] = useState("");
  const [activeOnly, setActiveOnly] = useState(true);
  const [sort, setSort] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const DEFAULT_META: PageMeta = { totalElements: 0, page: 0, size: 10 };
  const [items, setItems] = useState<User[]>([]);
  const [meta, setMeta] = useState<PageMeta>(DEFAULT_META);
  const [loadingList, setLoadingList] = useState(false);
  const [listErr, setListErr] = useState<string | null>(null);

  const totalPages = useMemo(() => {
    const m = meta ?? DEFAULT_META;
    return Math.max(1, Math.ceil(Math.max(0, m.totalElements) / Math.max(1, m.size)));
  }, [meta]);

  // --- Stats ---
  const [stats, setStats] = useState<UserStats | null>(null);

  // --- Delete confirm modal ---
  const [confirm, setConfirm] = useState<{ open: boolean; user?: User; soft?: boolean }>({ open: false });

  async function loadList(params?: Partial<{ q: string; activeOnly: boolean; page: number; size: number; sort?: string }>) {
    const _q = params?.q ?? q;
    const _activeOnly = params?.activeOnly ?? activeOnly;
    const _page = params?.page ?? page;
    const _size = params?.size ?? size;
    const _sort = params?.sort ?? sort;

    const qp = new URLSearchParams();
    qp.set("page", String(_page));
    qp.set("size", String(_size));
    qp.set("activeOnly", String(_activeOnly));
    if (_q) qp.set("q", _q);
    if (_sort) qp.set("sort", _sort);

    setLoadingList(true);
    setListErr(null);
    try {
      const payload = await apiGet<PagedResponse<User>>(`/api/users?${qp.toString()}`);
      if (!payload.success) throw new Error(payload.error?.message || "Failed");

      const data = (payload.data ?? { items: [], meta: { ...DEFAULT_META, page: _page, size: _size } }) as PagedResponse<User>;
      setItems(Array.isArray(data.items) ? data.items : []);
      setMeta(data.meta ?? { ...DEFAULT_META, page: _page, size: _size });
    } catch (e: any) {
      setListErr(e.message || "Failed loading list");
      setMeta((prev) => prev ?? DEFAULT_META);
    } finally {
      setLoadingList(false);
    }
  }

  async function loadStats() {
    const payload = await apiGet<UserStats>("/api/users/stats");
    if (payload.success && payload.data) setStats(payload.data);
  }

  useEffect(() => {
    loadList({ page: 0 });
    loadStats();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function onCreate(e: React.FormEvent) {
    e.preventDefault();
    setCreating(true);
    try {
      const payload = await apiPost<User>("/api/users", { firstName, lastName, email, password });
      if (!payload.success) throw new Error(payload.error?.message || payload.error?.code || "Create failed");
      setToast({ msg: "User created", tone: "green" });
      setFirstName("");
      setLastName("");
      setEmail("");
      setPassword("");
      await Promise.all([loadList({ page: 0 }), loadStats()]);
    } catch (e: any) {
      setToast({ msg: e.message || "Create failed", tone: "red" });
      console.debug("Create error:", e);
    } finally {
      setCreating(false);
    }
  }

  async function onDeleteConfirmed() {
    if (!confirm.user) return;
    const u = confirm.user;
    const soft = !!confirm.soft;
    const payload = await apiDelete<string>(`/api/users/${u.id}?soft=${soft}`);
    if (!payload.success) {
      setToast({ msg: payload.error?.message || payload.error?.code || "Delete failed", tone: "red" });
    } else {
      setToast({ msg: soft ? "Soft deleted" : "Hard deleted", tone: "green" });
      await Promise.all([loadList({ page }), loadStats()]);
    }
    setConfirm({ open: false });
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-6 pb-6 pt-8">
        <div className="flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-2xl bg-slate-900 text-white">UA</div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">User Admin</h1>
            <p className="text-xs text-slate-500">Spring Boot API · React Frontend</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Badge tone="blue">{new Date().toLocaleDateString()}</Badge>
          {stats && (
            <Badge tone="green">
              Last 24h: <span className="ml-1 font-semibold">{stats.createdLast24h}</span>
            </Badge>
          )}
        </div>
      </header>

      <main className="mx-auto max-w-6xl space-y-6 px-6 pb-10">
        {/* Create user */}
        <Card>
          <SectionTitle title="Create user" subtitle="Add a new user to the system" />
          <form onSubmit={onCreate} className="grid grid-cols-1 gap-3 md:grid-cols-5">
            <Input placeholder="First name" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
            <Input placeholder="Last name" value={lastName} onChange={(e) => setLastName(e.target.value)} />
            <Input placeholder="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            <Input placeholder="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            <div className="flex items-center justify-end gap-2">
              <Button type="submit" disabled={creating}>
                {creating ? (
                  <span className="flex items-center gap-2">
                    <Spinner /> Creating…
                  </span>
                ) : (
                  "Create"
                )}
              </Button>
            </div>
          </form>
        </Card>

        {/* Toolbar */}
        <Card className="flex flex-wrap items-center gap-3">
          <Input
            className="w-full max-w-sm"
            placeholder="Search (name/email)"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            leftIcon={<span className="text-slate-400">🔎</span>}
          />
          <Checkbox checked={activeOnly} onChange={setActiveOnly} label="Active only" />
          <Select value={sort ?? ""} onChange={(e) => setSort(e.target.value || undefined)}>
            <option value="">Sort: default (createdAt desc, id desc)</option>
            <option value="firstName,asc">firstName ↑</option>
            <option value="firstName,desc">firstName ↓</option>
            <option value="lastName,asc">lastName ↑</option>
            <option value="lastName,desc">lastName ↓</option>
          </Select>
          <Select value={size} onChange={(e) => setSize(parseInt(e.target.value))}>
            {[10, 20, 50, 100].map((n) => (
              <option key={n} value={n}>
                Page size: {n}
              </option>
            ))}
          </Select>
          <Button
            variant="secondary"
            onClick={() => {
              setPage(0);
              loadList({ q, activeOnly, page: 0, size, sort });
            }}
          >
            Apply
          </Button>
          <div className="ml-auto flex items-center gap-2 text-sm text-slate-600">
            <span>
              Page <span className="font-semibold text-slate-900">{(meta?.page ?? 0) + 1}</span> / {totalPages} · {meta?.totalElements ?? 0} total
            </span>
            <Button
              variant="ghost"
              onClick={() => {
                const p = Math.max(0, (meta?.page ?? 0) - 1);
                setPage(p);
                loadList({ page: p });
              }}
              disabled={(meta?.page ?? 0) <= 0 || loadingList}
            >
              Prev
            </Button>
            <Button
              variant="ghost"
              onClick={() => {
                const p = Math.min(totalPages - 1, (meta?.page ?? 0) + 1);
                setPage(p);
                loadList({ page: p });
              }}
              disabled={(meta?.page ?? 0) >= totalPages - 1 || loadingList}
            >
              Next
            </Button>
          </div>
        </Card>

        {/* List */}
        <Card>
          <SectionTitle title="Users" subtitle={loadingList ? "Loading list…" : `${meta?.totalElements ?? 0} total`} />
          {listErr && <div className="mb-3 rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">{listErr}</div>}
          <div className="overflow-x-auto">
            <table className="min-w-full overflow-hidden rounded-2xl text-left text-sm">
              <thead>
                <tr className="bg-slate-50 text-slate-700">
                  <th className="p-3 font-semibold">Email</th>
                  <th className="p-3 font-semibold">Name</th>
                  <th className="p-3 font-semibold">Active</th>
                  <th className="p-3 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {items.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50">
                    <td className="p-3 font-medium text-slate-800">{u.email}</td>
                    <td className="p-3">
                      {u.firstName} {u.lastName}
                    </td>
                    <td className="p-3">
                      {u.active ? <Badge tone="green">active</Badge> : <Badge tone="slate">inactive</Badge>}
                    </td>
                    <td className="p-3">
                      <div className="flex gap-2">
                        <Button variant="danger" onClick={() => setConfirm({ open: true, user: u, soft: true })}>
                          Soft delete
                        </Button>
                        <Button variant="ghost" onClick={() => setConfirm({ open: true, user: u, soft: false })}>
                          Hard delete
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr>
                    <td colSpan={4} className="p-6 text-center text-slate-500">
                      {loadingList ? (
                        <span className="inline-flex items-center gap-2">
                          <Spinner /> Loading…
                        </span>
                      ) : (
                        "No results"
                      )}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </main>

      {/* Modal + Toast */}
      <Modal open={confirm.open} onClose={() => setConfirm({ open: false })} title={confirm.soft ? "Soft delete user" : "Hard delete user"}>
        <p className="mb-4 text-sm text-slate-600">
          Are you sure you want to {confirm.soft ? "soft delete" : "hard delete"} <span className="font-semibold">{confirm.user?.email}</span>?
        </p>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setConfirm({ open: false })}>
            Cancel
          </Button>
          <Button variant={confirm.soft ? "danger" : "primary"} onClick={onDeleteConfirmed}>
            {confirm.soft ? "Soft delete" : "Hard delete"}
          </Button>
        </div>
      </Modal>

      {toast && <Toast message={toast.msg} tone={toast.tone} onClose={() => setToast(null)} />}

      <footer className="px-6 py-8 text-center text-xs text-slate-500">React + Tailwind · talks to /api/users</footer>
    </div>
  );
}
