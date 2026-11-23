// HTTP layer, with injected auth so it is testable without network. Same shape as verify-stamp/handler.ts.
import { type AccountAdmin, deleteAccount, isConfirmed } from "./account.ts";

export interface HandlerDeps {
  /** User id from the verified JWT, or null when missing/invalid. Never read from the body. */
  authenticate(req: Request): Promise<string | null>;
  admin: AccountAdmin;
}

const json = (body: unknown, status: number) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

/**
 * POST with `Authorization: Bearer <user JWT>` and body `{ "confirm": "HAPUS" }`.
 * 200 `{ status: "deleted" }`, 400 `{ error: "bad_request" }`, 401 `{ error: "unauthorized" }`,
 * 405 `{ error: "method_not_allowed" }`, 500 `{ error: "server_error" }`.
 * Only the caller's own account can be deleted: the id is taken from the JWT and any id in the body is ignored.
 */
export async function handleRequest(req: Request, deps: HandlerDeps): Promise<Response> {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const userId = await deps.authenticate(req);
  if (!userId) return json({ error: "unauthorized" }, 401);

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    body = undefined;
  }
  if (!isConfirmed(body)) return json({ error: "bad_request" }, 400);

  try {
    await deleteAccount(userId, deps.admin);
  } catch {
    // No details and no user id in logs.
    console.error(JSON.stringify({ fn: "delete-account", fault: true }));
    return json({ error: "server_error" }, 500);
  }
  console.log(JSON.stringify({ fn: "delete-account", status: "deleted" }));
  return json({ status: "deleted" }, 200);
}
