"use server";

import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";
import { FIELDS, errorCode, isFactKind, jakartaDate, parseField, type FactKind } from "@/lib/facts";

// ------------------------------------------------------------------ auth
export async function signIn(formData: FormData) {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  if (!email || !password) redirect("/masuk?galat=kosong");
  const supabase = await createClient();
  const { error } = await supabase.auth.signInWithPassword({ email, password });
  if (error) redirect("/masuk?galat=gagal");
  redirect("/");
}

export async function signOut() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/masuk");
}

// ------------------------------------------------------------------ facts
/** Only same-site relative paths, so the form cannot redirect elsewhere. */
function safeReturn(v: FormDataEntryValue | null): string {
  const s = typeof v === "string" ? v : "/";
  return s.startsWith("/") && !s.startsWith("//") ? s : "/";
}

function withParam(path: string, key: string, value: string): string {
  const u = new URL(path, "http://x");
  u.searchParams.set(key, value);
  return `${u.pathname}${u.search}`;
}

function readTarget(formData: FormData): { kind: FactKind; id: string; back: string } {
  const kind = formData.get("kind");
  const id = String(formData.get("id") ?? "");
  const back = safeReturn(formData.get("return"));
  if (!isFactKind(kind) || !/^[0-9a-f-]{36}$/i.test(id)) redirect(withParam(back, "galat", "fact_not_found"));
  return { kind, id, back };
}

/** Values changed in the form, as the patch for staff_save_fact. The database decides what is content. */
function buildPatch(kind: FactKind, formData: FormData): Record<string, unknown> {
  const patch: Record<string, unknown> = {};
  for (const def of FIELDS[kind]) {
    patch[def.name] = parseField(def, formData.get(def.name));
  }
  const source = String(formData.get("source_url") ?? "").trim();
  patch.source_url = source === "" ? null : source;
  // "Tanggal periksa" is a date; only send it when it changed, so the stored time is kept otherwise.
  const checkedDate = String(formData.get("checked_date") ?? "");
  const previous = jakartaDate(String(formData.get("checked_at_prev") ?? "") || null);
  if (checkedDate !== previous) {
    patch.checked_at = checkedDate ? `${checkedDate}T00:00:00+07:00` : null;
  }
  return patch;
}

async function save(kind: FactKind, id: string, formData: FormData) {
  const supabase = await createClient();
  const dueOn = String(formData.get("due_on") ?? "");
  return supabase.rpc("staff_save_fact", {
    p_kind: kind,
    p_id: id,
    p_patch: buildPatch(kind, formData),
    p_internal_note: String(formData.get("internal_note") ?? ""),
    p_manager_confirmation: String(formData.get("manager_confirmation") ?? ""),
    p_has_conflict: formData.get("has_conflict") === "on",
    p_due_on: dueOn === "" ? null : dueOn,
  });
}

export async function saveDraft(formData: FormData) {
  const { kind, id, back } = readTarget(formData);
  const { error } = await save(kind, id, formData);
  revalidatePath("/", "layout");
  redirect(withParam(back, error ? "galat" : "ok", error ? errorCode(error.message) : "draf"));
}

/** "Minta tinjauan": save, then mark the current content as checked by me. */
export async function requestReview(formData: FormData) {
  const { kind, id, back } = readTarget(formData);
  const saved = await save(kind, id, formData);
  if (saved.error) redirect(withParam(back, "galat", errorCode(saved.error.message)));
  const supabase = await createClient();
  const { error } = await supabase.rpc("mark_fact_checked", { p_kind: kind, p_id: id });
  revalidatePath("/", "layout");
  redirect(withParam(back, error ? "galat" : "ok", error ? errorCode(error.message) : "diperiksa"));
}

/** "Setujui dan terbitkan": the second pair of eyes. The database refuses the checker. */
export async function publish(formData: FormData) {
  const { kind, id, back } = readTarget(formData);
  const supabase = await createClient();
  const { error } = await supabase.rpc("publish_fact", { p_kind: kind, p_id: id });
  revalidatePath("/", "layout");
  redirect(withParam(back, error ? "galat" : "ok", error ? errorCode(error.message) : "terbit"));
}
