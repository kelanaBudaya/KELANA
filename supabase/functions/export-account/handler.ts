// HTTP layer, with injected auth so it is testable without network. Same shape as delete-account/handler.ts.
import { type AccountExport, type AccountUser, buildExport, type ExportSource } from "./export.ts";

export interface Caller {
  user: AccountUser;
  /** Reads with the caller's own JWT (RLS applies), never with the service role. */
  source: ExportSource;
}

export interface HandlerDeps {
  /** The verified caller from the JWT, or null when missing/invalid. Never read from the body. */
  authenticate(req: Request): Promise<Caller | null>;
  now(): Date;
}

const json = (body: unknown, status: number, pretty = false) =>
  new Response(JSON.stringify(body, null, pretty ? 2 : undefined), {
    status,
    // The answer is personal data: no shared or browser cache may keep it.
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });

/**
 * POST with `Authorization: Bearer <user JWT>`. The body is ignored (a user id in it has no effect).
 * 200 the export document (see export.ts, pretty-printed), 401 `{ error: "unauthorized" }`,
 * 405 `{ error: "method_not_allowed" }`, 500 `{ error: "server_error" }`.
 */
export async function handleRequest(req: Request, deps: HandlerDeps): Promise<Response> {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const caller = await deps.authenticate(req);
  if (!caller) return json({ error: "unauthorized" }, 401);

  let doc: AccountExport;
  try {
    doc = await buildExport(caller.user, caller.source, deps.now());
  } catch {
    // No details and no user id in logs.
    console.error(JSON.stringify({ fn: "export-account", fault: true }));
    return json({ error: "server_error" }, 500);
  }
  console.log(JSON.stringify({ fn: "export-account", status: "exported" }));
  return json(doc, 200, true);
}
