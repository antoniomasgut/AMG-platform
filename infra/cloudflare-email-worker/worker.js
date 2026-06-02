/**
 * Cloudflare Email Worker — AMG Bot Inbound Email
 *
 * Rep emails entrants a *@inbound.amgdl.com i els reenvia al backend d'AMG.
 * El backend enruta al tenant correcte per l'adreça de destinació (To).
 *
 * Deploy:
 *   npx wrangler deploy
 *
 * Configuració a Cloudflare Dashboard:
 *   Email → Email Routing → Rules → Catch-all → Send to Worker → amg-email-worker
 */

export default {
  async email(message, env, ctx) {
    try {
      const rawEmail = await streamToText(message.raw);
      const bodyText = extractPlainText(rawEmail);

      const body = new URLSearchParams({
        recipient: message.to,
        sender:    message.from,
        'body-plain': bodyText || '(sense cos)',
      });

      const response = await fetch('https://api.amgdl.com/api/v1/agents/email/inbound', {
        method:  'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body:    body.toString(),
      });

      if (!response.ok) {
        console.error(`AMG backend error: ${response.status} ${await response.text()}`);
      } else {
        console.log(`Email enrutat: from=${message.from} to=${message.to}`);
      }
    } catch (err) {
      console.error('Email worker error:', err);
    }
  },
};

// ── Llegir un ReadableStream com a text ─────────────────────────
async function streamToText(stream) {
  const reader  = stream.getReader();
  const decoder = new TextDecoder();
  let result = '';
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    result += decoder.decode(value, { stream: true });
  }
  result += decoder.decode();
  return result;
}

// ── Extreure el text pla d'un email RFC 2822 ────────────────────
function extractPlainText(rawEmail) {
  // Separar capçaleres del cos
  const sep = rawEmail.indexOf('\r\n\r\n');
  if (sep === -1) return rawEmail.trim();
  const headers = rawEmail.substring(0, sep);
  const body    = rawEmail.substring(sep + 4);

  // Multipart: cercar la part text/plain
  const boundary = (headers.match(/boundary="?([^"\r\n;]+)"?/i) || [])[1];
  if (boundary) {
    const parts = body.split(new RegExp('--' + escapeRegex(boundary) + '(?:--)?'));
    for (const part of parts) {
      const partSep = part.indexOf('\r\n\r\n');
      if (partSep === -1) continue;
      const partHeaders = part.substring(0, partSep);
      const partBody    = part.substring(partSep + 4);
      if (/content-type:\s*text\/plain/i.test(partHeaders)) {
        return decodeBody(partBody, partHeaders).trim();
      }
    }
    // Si no hi ha text/plain, treure HTML de la primera part HTML
    for (const part of parts) {
      const partSep = part.indexOf('\r\n\r\n');
      if (partSep === -1) continue;
      const partHeaders = part.substring(0, partSep);
      const partBody    = part.substring(partSep + 4);
      if (/content-type:\s*text\/html/i.test(partHeaders)) {
        return stripHtml(decodeBody(partBody, partHeaders)).trim();
      }
    }
  }

  // Single part
  if (/content-type:\s*text\/html/i.test(headers)) {
    return stripHtml(decodeBody(body, headers)).trim();
  }
  return decodeBody(body, headers).trim();
}

function decodeBody(body, headers) {
  const enc = ((headers || '').match(/content-transfer-encoding:\s*(\S+)/i) || [])[1] || '';
  if (/base64/i.test(enc)) {
    try { return atob(body.replace(/\s/g, '')); } catch { return body; }
  }
  if (/quoted-printable/i.test(enc)) {
    return body
      .replace(/=\r?\n/g, '')
      .replace(/=([0-9A-Fa-f]{2})/g, (_, h) => String.fromCharCode(parseInt(h, 16)));
  }
  return body;
}

function stripHtml(html) {
  return html.replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
             .replace(/<[^>]+>/g, ' ')
             .replace(/&nbsp;/g, ' ')
             .replace(/&amp;/g, '&')
             .replace(/&lt;/g, '<')
             .replace(/&gt;/g, '>')
             .replace(/\s{2,}/g, ' ');
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
