import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import cors from "cors";
import * as crypto from "crypto";
import sgMail from "@sendgrid/mail";

admin.initializeApp();
const db = admin.firestore();
const runtimeOpts: functions.RuntimeOptions = { memory: "256MB", timeoutSeconds: 30 };

const APP_SCHEME = "edgeone";
const TOKEN_TTL_MS = 24 * 60 * 60 * 1000; // 24h

// Read from environment config: functions:config:set sendgrid.key=... app.admin_email=...
const SENDGRID_KEY = process.env.SENDGRID_API_KEY || functions.config().sendgrid?.key;
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || functions.config().app?.admin_email;
const WEB_BASE = process.env.APP_WEB_BASE || functions.config().app?.web_base || "";
if (SENDGRID_KEY) sgMail.setApiKey(SENDGRID_KEY);

function hashToken(input: string): string {
  return crypto.createHash("sha256").update(input).digest("hex");
}

function makeActionUrl(baseUrl: string, requestId: string, action: "approve" | "reject", token: string) {
  const url = new URL(baseUrl);
  url.searchParams.set("request_id", requestId);
  url.searchParams.set("token", token);
  url.searchParams.set("action", action);
  return url.toString();
}

function appDeepLink(requestId: string, action: "approve" | "reject", token: string) {
  return `${APP_SCHEME}://approve-token?id=${encodeURIComponent(requestId)}&action=${action}&token=${encodeURIComponent(token)}`;
}

export const requestTokens = functions
  .runWith(runtimeOpts)
  .https.onRequest(async (req, res) => {
    cors({ origin: true })(req, res, async () => {
      try {
        if (req.method !== "POST") {
          res.status(405).send("Method Not Allowed");
          return;
        }
        const { userId, email, amount = 50 } = req.body || {};
        if (!userId || !email) {
          res.status(400).send("Missing userId/email");
          return;
        }

        const docRef = db.collection("token_requests").doc();
        const plainToken = crypto.randomBytes(32).toString("hex");
        const tokenHash = hashToken(plainToken);
        const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + TOKEN_TTL_MS);

        const data = {
          id: docRef.id,
          userId,
          email,
          amount,
          status: "pending",
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          tokenHash,
          expiresAt,
        };
        await docRef.set(data);

        if (!SENDGRID_KEY || !ADMIN_EMAIL) {
          functions.logger.warn("Email not configured; skipping email send");
          res.status(201).json({ id: docRef.id });
          return;
        }

        const runtimeBase = WEB_BASE || (req.headers["x-forwarded-proto"] ?
          `${req.headers["x-forwarded-proto"]}://${req.headers["x-forwarded-host"] || req.headers.host}` :
          `https://${req.headers.host}`);
        const approveUrl = makeActionUrl(`${runtimeBase}/approve`, docRef.id, "approve", plainToken);
        const rejectUrl = makeActionUrl(`${runtimeBase}/reject`, docRef.id, "reject", plainToken);

        const msg = {
          to: ADMIN_EMAIL,
          from: ADMIN_EMAIL,
          subject: `EdgeOne Credit Request: ${docRef.id}`,
          text: `User ${email} requested +${amount} credits.\nRequest ID: ${docRef.id}.\n\nApprove (web): ${approveUrl}\nReject (web): ${rejectUrl}\n\nIn-app: ${appDeepLink(docRef.id, "approve", plainToken)} or ${appDeepLink(docRef.id, "reject", plainToken)}`,
        };
        await sgMail.send(msg as any);
        res.status(201).json({ id: docRef.id });
      } catch (e: any) {
        functions.logger.error(e);
        res.status(500).send("Internal Error");
      }
    });
  });

async function verifyAndConsumeToken(requestId: string, token: string) {
  const snap = await db.collection("token_requests").doc(requestId).get();
  if (!snap.exists) return { ok: false, code: "not_found" as const };
  const data = snap.data()!;
  if (data.status !== "pending") return { ok: false, code: "not_pending" as const };
  const expiresAt = (data.expiresAt as admin.firestore.Timestamp).toMillis();
  if (Date.now() > expiresAt) return { ok: false, code: "expired" as const };
  const tokenHash = data.tokenHash as string;
  if (hashToken(token) !== tokenHash) return { ok: false, code: "invalid_token" as const };
  return { ok: true, data } as const;
}

async function applyApproval(userId: string, amount: number, requestId: string, approvedBy: string) {
  const reqRef = db.collection("token_requests").doc(requestId);
  const creditsRef = db.collection("users").doc(userId).collection("meta").doc("credits");
  await db.runTransaction(async (txn) => {
    const reqSnap = await txn.get(reqRef);
    if (!reqSnap.exists) throw new Error("missing");
    if (reqSnap.get("status") !== "pending") throw new Error("not_pending");
    const credSnap = await txn.get(creditsRef);
    const remaining = (credSnap.get("remaining") || 0) as number;
    const used = (credSnap.get("used") || 0) as number;
    txn.set(creditsRef, { remaining: remaining + amount, used }, { merge: true });
    txn.update(reqRef, {
      status: "approved",
      approvedAt: admin.firestore.FieldValue.serverTimestamp(),
      approvedBy,
      tokenHash: admin.firestore.FieldValue.delete(),
    });
  });
}

async function applyRejection(requestId: string, rejectedBy: string) {
  const reqRef = db.collection("token_requests").doc(requestId);
  await reqRef.update({
    status: "rejected",
    rejectedAt: admin.firestore.FieldValue.serverTimestamp(),
    rejectedBy,
    tokenHash: admin.firestore.FieldValue.delete(),
  });
}

export const approve = functions.runWith(runtimeOpts).https.onRequest(async (req, res) => {
  cors({ origin: true })(req, res, async () => {
    try {
      const { request_id, token } = (req.method === "GET" ? req.query : req.body) as any;
      if (!request_id || !token) { res.status(400).send("missing"); return; }
      const check = await verifyAndConsumeToken(String(request_id), String(token));
      if (!check.ok) { res.status(400).send(check.code); return; }
      const { userId, amount } = check.data as any;
      await applyApproval(userId, amount, String(request_id), ADMIN_EMAIL || "web-approve@functions");
      res.status(200).send("approved");
    } catch (e) {
      functions.logger.error(e);
      res.status(500).send("error");
    }
  });
});

export const reject = functions.runWith(runtimeOpts).https.onRequest(async (req, res) => {
  cors({ origin: true })(req, res, async () => {
    try {
      const { request_id, token } = (req.method === "GET" ? req.query : req.body) as any;
      if (!request_id || !token) { res.status(400).send("missing"); return; }
      const check = await verifyAndConsumeToken(String(request_id), String(token));
      if (!check.ok) { res.status(400).send(check.code); return; }
      await applyRejection(String(request_id), ADMIN_EMAIL || "web-reject@functions");
      res.status(200).send("rejected");
    } catch (e) {
      functions.logger.error(e);
      res.status(500).send("error");
    }
  });
});

export const status = functions.runWith(runtimeOpts).https.onRequest(async (req, res) => {
  cors({ origin: true })(req, res, async () => {
    try {
      const { request_id } = (req.method === "GET" ? req.query : req.body) as any;
      if (!request_id) { res.status(400).send("missing"); return; }
      const snap = await db.collection("token_requests").doc(String(request_id)).get();
      if (!snap.exists) { res.status(404).send("not_found"); return; }
      res.status(200).json(snap.data());
    } catch (e) {
      functions.logger.error(e);
      res.status(500).send("error");
    }
  });
});

// Server-authoritative credit consumption
export const consumeCredits = functions
  .runWith(runtimeOpts)
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Login required");
    }
    const uid = context.auth.uid;
    const amountRaw = (data && (data as any).amount) as unknown;
    const amount = typeof amountRaw === "number" ? amountRaw : parseInt(String(amountRaw || 1), 10);
    if (!Number.isInteger(amount) || amount <= 0) {
      throw new functions.https.HttpsError("invalid-argument", "amount must be a positive integer");
    }

    const creditsRef = db.collection("users").doc(uid).collection("meta").doc("credits");
    try {
      await db.runTransaction(async (txn) => {
        const snap = await txn.get(creditsRef);
        const used = ((snap.get("used") || 0) as number) | 0;
        const remaining = ((snap.get("remaining") || 0) as number) | 0;
        if (remaining < amount) {
          throw new functions.https.HttpsError("failed-precondition", "insufficient_credits");
        }
        txn.set(
          creditsRef,
          { used: used + amount, remaining: remaining - amount },
          { merge: true }
        );
      });
      return { ok: true };
    } catch (e: any) {
      if (e instanceof functions.https.HttpsError) throw e;
      throw new functions.https.HttpsError("internal", e?.message || "consume failed");
    }
  });

export const onTokenRequestCreated = functions.runWith(runtimeOpts).firestore
  .document("token_requests/{id}")
  .onCreate(async (snap) => {
    try {
      const data = snap.data() || {} as any;
      if (data.tokenHash) return; // already processed
      const plainToken = crypto.randomBytes(32).toString("hex");
      const tokenHash = hashToken(plainToken);
      const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + TOKEN_TTL_MS);
      await snap.ref.update({ tokenHash, expiresAt });

      if (!SENDGRID_KEY || !ADMIN_EMAIL) {
        functions.logger.warn("Email not configured; skipping email send");
        return;
      }

      const approveUrl = WEB_BASE ? makeActionUrl(`${WEB_BASE}/approve`, snap.id, "approve", plainToken) : undefined;
      const rejectUrl = WEB_BASE ? makeActionUrl(`${WEB_BASE}/reject`, snap.id, "reject", plainToken) : undefined;
      const msg = {
        to: ADMIN_EMAIL,
        from: ADMIN_EMAIL,
        subject: `EdgeOne Credit Request: ${snap.id}`,
        text: `User ${data.email || data.userId} requested +${data.amount} credits.\nRequest ID: ${snap.id}.\n\n` +
          (approveUrl && rejectUrl ? `Approve (web): ${approveUrl}\nReject (web): ${rejectUrl}\n\n` : "") +
          `In-app: ${appDeepLink(snap.id, "approve", plainToken)} or ${appDeepLink(snap.id, "reject", plainToken)}`,
      };
      await sgMail.send(msg as any);
    } catch (e) {
      functions.logger.error(e);
    }
  });


