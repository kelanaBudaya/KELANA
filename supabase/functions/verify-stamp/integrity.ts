// Play Integrity verification behind the IntegrityVerifier interface.
// The real verifier is NOT exercised without a Google Cloud setup (service account + Play Integrity API enabled).
import { EXPECTED_PACKAGE_NAME, type IntegrityVerifier } from "./verify.ts";

export const DEV_TOKEN = "dev-token";

/** Dev stub: accepts only the literal token "dev-token". Used only when KELANA_ENV=dev. */
export class DevStubVerifier implements IntegrityVerifier {
  verify(token: string, _expectedNonce: string): Promise<boolean> {
    return Promise.resolve(token === DEV_TOKEN);
  }
}

/** Always fails: used when no secret is configured outside dev (fail closed). */
export class FailClosedVerifier implements IntegrityVerifier {
  verify(_token: string, _expectedNonce: string): Promise<boolean> {
    return Promise.resolve(false);
  }
}

/** Pure check of a decoded Play Integrity payload (tokenPayloadExternal). */
export function payloadIsValid(
  payload: unknown,
  expectedNonce: string,
  packageName = EXPECTED_PACKAGE_NAME,
): boolean {
  const p = payload as {
    requestDetails?: { nonce?: string; requestPackageName?: string };
    appIntegrity?: { appRecognitionVerdict?: string; packageName?: string };
    deviceIntegrity?: { deviceRecognitionVerdict?: string[] };
  } | null;
  if (!p) return false;
  if (p.requestDetails?.nonce !== expectedNonce) return false;
  if (p.requestDetails?.requestPackageName !== packageName) return false;
  if (p.appIntegrity?.packageName !== packageName) return false;
  if (p.appIntegrity?.appRecognitionVerdict !== "PLAY_RECOGNIZED") return false;
  return p.deviceIntegrity?.deviceRecognitionVerdict?.includes("MEETS_DEVICE_INTEGRITY") === true;
}

function b64url(bytes: Uint8Array): string {
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function pemToDer(pem: string): Uint8Array<ArrayBuffer> {
  const body = pem.replace(/-----[A-Z ]+-----/g, "").replace(/\s+/g, "");
  return Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
}

/** Calls Google decodeIntegrityToken with a service account. Not tested against Google. */
export class GoogleIntegrityVerifier implements IntegrityVerifier {
  constructor(
    private readonly serviceAccountJson: string,
    private readonly packageName = EXPECTED_PACKAGE_NAME,
    private readonly fetchFn: typeof fetch = fetch,
  ) {}

  private async accessToken(): Promise<string> {
    const sa = JSON.parse(this.serviceAccountJson) as { client_email: string; private_key: string };
    const enc = new TextEncoder();
    const now = Math.floor(Date.now() / 1000);
    const header = b64url(enc.encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
    const claims = b64url(enc.encode(JSON.stringify({
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/playintegrity",
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    })));
    const key = await crypto.subtle.importKey(
      "pkcs8",
      pemToDer(sa.private_key),
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["sign"],
    );
    const sig = new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, enc.encode(`${header}.${claims}`)));
    const res = await this.fetchFn("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion: `${header}.${claims}.${b64url(sig)}`,
      }),
    });
    if (!res.ok) throw new Error("token_exchange_failed");
    return (await res.json()).access_token as string;
  }

  async verify(token: string, expectedNonce: string): Promise<boolean> {
    try {
      const at = await this.accessToken();
      const res = await this.fetchFn(
        `https://playintegrity.googleapis.com/v1/${this.packageName}:decodeIntegrityToken`,
        {
          method: "POST",
          headers: { authorization: `Bearer ${at}`, "content-type": "application/json" },
          body: JSON.stringify({ integrity_token: token }),
        },
      );
      if (!res.ok) return false;
      const json = await res.json();
      return payloadIsValid(json.tokenPayloadExternal, expectedNonce, this.packageName);
    } catch {
      return false;
    }
  }
}

/** Choose the verifier from env. Fail closed unless a secret exists or KELANA_ENV=dev. */
export function verifierFromEnv(env: { get(k: string): string | undefined }): IntegrityVerifier {
  const sa = env.get("PLAY_INTEGRITY_SERVICE_ACCOUNT_JSON");
  if (sa) return new GoogleIntegrityVerifier(sa, env.get("PLAY_INTEGRITY_PACKAGE_NAME") ?? EXPECTED_PACKAGE_NAME);
  if (env.get("KELANA_ENV") === "dev") return new DevStubVerifier();
  return new FailClosedVerifier();
}
