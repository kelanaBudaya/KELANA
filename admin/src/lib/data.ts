import "server-only";
import { cache } from "react";
import { createClient } from "@/lib/supabase/server";
import type { HistoryRow, QueueRow, StaffRole } from "@/lib/facts";

export interface CurrentStaff {
  userId: string;
  email: string | null;
  role: StaffRole;
  displayName: string;
  regionName: string | null;
  regionCode: string | null;
}

export interface Summary {
  due_this_week: number;
  conflicts: number;
  pending: number;
  reports_7d: number;
  published_30d: number;
}

/** The signed-in user and their staff row (null when the account is not staff). */
export const getSession = cache(async () => {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { user: null, staff: null as CurrentStaff | null };
  const { data } = await supabase
    .from("staff")
    .select("user_id, role, display_name, regions(name, code)")
    .eq("user_id", user.id)
    .maybeSingle();
  if (!data) return { user, staff: null as CurrentStaff | null };
  const region = (Array.isArray(data.regions) ? data.regions[0] : data.regions) as
    | { name: string; code: string }
    | null
    | undefined;
  const staff: CurrentStaff = {
    userId: user.id,
    email: user.email ?? null,
    role: data.role as StaffRole,
    displayName: data.display_name as string,
    regionName: region?.name ?? null,
    regionCode: region?.code ?? null,
  };
  return { user, staff };
});

export const getQueue = cache(async (): Promise<QueueRow[]> => {
  const supabase = await createClient();
  const { data, error } = await supabase.rpc("staff_fact_queue");
  if (error) throw new Error(error.message);
  return (data ?? []) as QueueRow[];
});

export const getSummary = cache(async (): Promise<Summary> => {
  const supabase = await createClient();
  const { data, error } = await supabase.rpc("staff_summary");
  if (error) throw new Error(error.message);
  return data as Summary;
});

export const getStaffNames = cache(async (): Promise<Map<string, string>> => {
  const supabase = await createClient();
  const { data } = await supabase.from("staff").select("user_id, display_name");
  return new Map((data ?? []).map((s) => [s.user_id as string, s.display_name as string]));
});

export async function getHistory(opts: { placeId?: string; limit?: number }): Promise<HistoryRow[]> {
  const supabase = await createClient();
  let q = supabase
    .from("fact_history")
    .select("id, fact_table, fact_id, place_id, action, before, after, source_url, changed_by, changed_at")
    .order("id", { ascending: false })
    .limit(opts.limit ?? 50);
  if (opts.placeId) q = q.eq("place_id", opts.placeId);
  const { data, error } = await q;
  if (error) throw new Error(error.message);
  return (data ?? []) as HistoryRow[];
}

export const getPlaceNames = cache(async (): Promise<Map<string, string>> => {
  const supabase = await createClient();
  const { data } = await supabase.from("places").select("id, name");
  return new Map((data ?? []).map((p) => [p.id as string, p.name as string]));
});
