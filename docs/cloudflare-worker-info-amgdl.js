// ─────────────────────────────────────────────────────────────────────────────
// Cloudflare Email Worker per a  info@amgdl.com   (autònom · sense dependències)
//
// Per cada correu que arriba a info@amgdl.com:
//   1) L'envia al BOT d'AMG (webhook) → el bot el llegeix, redacta i respon/avisa.
//   2) En reenvia una còpia a  amgdigitalitzacions@gmail.com  (còpia humana).
//
// Es pot ENGANXAR TAL QUAL al panell de Cloudflare (Workers & Pages → Create →
// Worker), no cal cap llibreria ni Wrangler.
//
// Requisits al panell (Email · Email Routing del domini amgdl.com):
//   - Email Routing ACTIVAT (afegeix els MX automàticament).
//   - Destí  amgdigitalitzacions@gmail.com  afegit i VERIFICAT (clicar l'enllaç al Gmail).
//   - Regla:  info@amgdl.com  →  "Send to a Worker"  →  aquest Worker.
// ─────────────────────────────────────────────────────────────────────────────

const BOT_WEBHOOK  = "https://api.amgdl.com/api/v1/agents/email/inbound";
const TENANT_INBOX = "amg@inbound.amgdl.com";          // el bot resol el tenant AMG per aquesta adreça
const HUMAN_COPY   = "amgdigitalitzacions@gmail.com";  // còpia humana

export default {
  async email(message, ctx) {
    // 1) Còpia humana a Gmail (no bloqueja si falla)
    try { await message.forward(HUMAN_COPY); } catch (e) { console.log("forward:", e); }

    // 2) Enviar el contingut al bot
    try {
      const raw  = await streamToText(message.raw);
      const text = extractPlainText(raw);
      const form = new URLSearchParams();
      form.set("recipient", TENANT_INBOX);
      form.set("sender", message.from);
      form.set("subject", message.headers.get("subject") || "");
      form.set("body-plain", text);
      await fetch(BOT_WEBHOOK, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: form.toString(),
      });
    } catch (e) { console.log("bot:", e); }
  },
};

async function streamToText(stream) {
  const chunks = []; const reader = stream.getReader();
  for (;;) { const { done, value } = await reader.read(); if (done) break; chunks.push(value); }
  let len = 0; for (const c of chunks) len += c.length;
  const buf = new Uint8Array(len); let o = 0;
  for (const c of chunks) { buf.set(c, o); o += c.length; }
  return new TextDecoder("utf-8").decode(buf);
}

// Extreu el text pla del correu (gestiona multipart, quoted-printable i base64 bàsics)
function extractPlainText(raw) {
  let body = raw;
  const bm = raw.match(/boundary="?([^"\r\n;]+)"?/i);
  if (bm) {
    const parts = raw.split("--" + bm[1]);
    let chosen = parts.find(p => /content-type:\s*text\/plain/i.test(p))
             || parts.find(p => /content-type:\s*text\/html/i.test(p));
    if (chosen) body = chosen;
  }
  const idx = body.search(/\r?\n\r?\n/);
  let content = idx >= 0 ? body.slice(idx) : body;
  if (/content-transfer-encoding:\s*quoted-printable/i.test(body)) {
    content = content.replace(/=\r?\n/g, "").replace(/=([0-9A-Fa-f]{2})/g, (_, h) => String.fromCharCode(parseInt(h, 16)));
  } else if (/content-transfer-encoding:\s*base64/i.test(body)) {
    try { content = atob(content.replace(/\s+/g, "")); } catch (e) {}
  }
  return content.replace(/<[^>]+>/g, " ").trim().slice(0, 4000);
}
