// HTTP layer, with injected auth so it is testable without network.
import { decide, type Deps, type VerifyResult } from "./verify.ts";

export interface HandlerDeps extends Deps {
  /** User id from the verified JWT, or null when missing/invalid. Never read from the body. */
  authenticate(req: Request): Promise<string | null>;
}

const json = (body: unknown, status: number) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

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
  let result: VerifyResult;
  try {
    result = await decide(body, userId, deps);
  } catch {
    // No error details: they could echo request input.
    console.error(JSON.stringify({ fn: "verify-stamp", fault: true }));
    return json({ error: "server_error" }, 500);
  }
  console.log(JSON.stringify({ fn: "verify-stamp", status: result.status, reason: result.reason ?? null }));
  return json(result, result.reason === "bad_request" ? 400 : 200);
}
