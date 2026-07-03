const BASE = `${process.env.NEXT_PUBLIC_API_URL ?? ''}/api/v1`;

export interface DocumentViewResponse {
  fileName: string;
  description: string | null;
  documentType: 'BUDGET' | 'INVOICE' | 'MEDICAL_REPORT' | 'GENERIC';
  htmlContent: string | null;
  canAccept: boolean;
  accepted: boolean;
  acceptedAt: string | null;
  signerName: string | null;
}

export const getDocumentView = (token: string): Promise<DocumentViewResponse> =>
  fetch(`${BASE}/documents/view/${token}`)
    .then(r => {
      if (!r.ok) throw new Error(r.status === 404 ? 'not_found' : r.status === 410 ? 'expired' : 'error');
      return r.json() as Promise<DocumentViewResponse>;
    });

export const acceptDocument = (token: string, signerName: string): Promise<{ paymentUrl: string | null }> =>
  fetch(`${BASE}/documents/view/${token}/accept`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ signerName }),
  }).then(async r => {
    if (!r.ok) {
      const text = await r.text();
      throw new Error(text || 'Error acceptant el document');
    }
    try {
      return await r.json() as { paymentUrl: string | null };
    } catch {
      return { paymentUrl: null };
    }
  });

export const downloadUrl = (token: string) => `${BASE}/documents/download/${token}`;
