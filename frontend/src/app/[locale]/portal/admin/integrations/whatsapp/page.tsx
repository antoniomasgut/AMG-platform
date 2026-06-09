'use client';

import { useState, useEffect, useCallback, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';
import {
  getWhatsAppConfig, getConnectUrl, getOAuthSession, confirmConnection, disconnectWhatsApp,
  type WabaConfig, type OAuthSession, type WabaChoice, type PhoneChoice,
} from '@/services/whatsapp-oauth';

function formatDate(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

// ── Connected state ──────────────────────────────────────────────────────────

function ConnectedCard({ config, onDisconnect }: { config: WabaConfig; onDisconnect: () => void }) {
  return (
    <div className="amg-card card-clip p-5 space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-green-950/40 border border-green-500/30 flex items-center justify-center">
            <IconSet.Check size={18} stroke="#4ade80" />
          </div>
          <div>
            <div className="f-display font-bold text-sm">WhatsApp Business connectat</div>
            <div className="f-mono text-caption text-ink-2">Connectat el {formatDate(config.connectedAt)}</div>
          </div>
        </div>
        <AMGBadge tone="success">ACTIU</AMGBadge>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-border-base">
        <div>
          <div className="f-mono text-label uppercase text-ink-3 mb-1">Número</div>
          <div className="f-display font-bold text-sm">{config.displayPhoneNumber || '—'}</div>
        </div>
        <div>
          <div className="f-mono text-label uppercase text-ink-3 mb-1">WABA ID</div>
          <div className="f-mono text-caption text-ink-1 break-all">{config.wabaId}</div>
        </div>
        <div>
          <div className="f-mono text-label uppercase text-ink-3 mb-1">Webhook</div>
          <AMGBadge tone={config.webhookRegistered ? 'success' : 'warning'}>
            {config.webhookRegistered ? 'Registrat' : 'Pendent'}
          </AMGBadge>
        </div>
        {config.businessName && (
          <div>
            <div className="f-mono text-label uppercase text-ink-3 mb-1">Empresa Meta</div>
            <div className="text-sm text-ink-1">{config.businessName}</div>
          </div>
        )}
      </div>

      <div className="pt-2">
        <AMGButton
          variant="ghost"
          size="sm"
          icon={IconSet.X}
          onClick={onDisconnect}
        >
          Desconnectar WhatsApp
        </AMGButton>
      </div>
    </div>
  );
}

// ── WABA selector ────────────────────────────────────────────────────────────

function WabaSelector({
  session,
  onConfirm,
  onCancel,
}: {
  session: OAuthSession;
  onConfirm: (wabaId: string, phoneNumberId: string) => void;
  onCancel: () => void;
}) {
  const [selectedWaba, setSelectedWaba] = useState<WabaChoice | null>(
    session.wabas.length === 1 ? session.wabas[0] : null
  );
  const [selectedPhone, setSelectedPhone] = useState<PhoneChoice | null>(null);
  const [confirming, setConfirming] = useState(false);

  useEffect(() => {
    if (selectedWaba?.phones.length === 1) setSelectedPhone(selectedWaba.phones[0]);
    else setSelectedPhone(null);
  }, [selectedWaba]);

  const handleConfirm = async () => {
    if (!selectedWaba || !selectedPhone) return;
    setConfirming(true);
    onConfirm(selectedWaba.wabaId, selectedPhone.phoneNumberId);
  };

  return (
    <div className="amg-card card-clip p-5 space-y-5">
      <div>
        <div className="f-display font-bold text-base">Selecciona el teu compte WhatsApp Business</div>
        <p className="text-sm text-ink-2 mt-1">Meta ha autoritzat l'accés als WABAs següents. Selecciona el que vols connectar.</p>
      </div>

      {/* WABA selector */}
      {session.wabas.length > 1 && (
        <div className="space-y-2">
          <div className="f-mono text-label uppercase text-ink-3">WhatsApp Business Account</div>
          <div className="space-y-2">
            {session.wabas.map(w => (
              <button
                key={w.wabaId}
                onClick={() => setSelectedWaba(w)}
                className={`w-full text-left p-3 border rounded-lg transition-colors ${
                  selectedWaba?.wabaId === w.wabaId
                    ? 'border-[#FF6B00] bg-accent-muted'
                    : 'border-border-base hover:border-border-strong'
                }`}
              >
                <div className="f-display font-bold text-sm">{w.businessName}</div>
                <div className="f-mono text-caption text-ink-3">{w.wabaId}</div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Phone selector */}
      {selectedWaba && selectedWaba.phones.length > 0 && (
        <div className="space-y-2">
          <div className="f-mono text-label uppercase text-ink-3">Número de telèfon</div>
          <div className="space-y-2">
            {selectedWaba.phones.map(p => (
              <button
                key={p.phoneNumberId}
                onClick={() => setSelectedPhone(p)}
                className={`w-full text-left p-3 border rounded-lg transition-colors ${
                  selectedPhone?.phoneNumberId === p.phoneNumberId
                    ? 'border-[#FF6B00] bg-accent-muted'
                    : 'border-border-base hover:border-border-strong'
                }`}
              >
                <div className="f-display font-bold text-sm">{p.displayPhone}</div>
                {p.verifiedName && (
                  <div className="f-mono text-caption text-ink-3">{p.verifiedName}</div>
                )}
              </button>
            ))}
          </div>
        </div>
      )}

      {selectedWaba && selectedWaba.phones.length === 0 && (
        <div className="p-3 bg-yellow-950/30 border border-yellow-500/30 rounded-lg text-sm text-yellow-400">
          Aquest WABA no té números de telèfon verificats.
        </div>
      )}

      <div className="flex gap-3 pt-2">
        <AMGButton
          size="sm"
          icon={IconSet.Check}
          onClick={handleConfirm}
          disabled={!selectedWaba || !selectedPhone || confirming}
        >
          {confirming ? 'Confirmant…' : 'Confirmar connexió'}
        </AMGButton>
        <AMGButton variant="ghost" size="sm" onClick={onCancel}>
          Cancel·lar
        </AMGButton>
      </div>
    </div>
  );
}

// ── Main page ────────────────────────────────────────────────────────────────

function WhatsAppIntegrationContent() {
  const { user } = useAuth();
  const { toast } = useToast();
  const router = useRouter();
  const searchParams = useSearchParams();
  const tenantId = user?.tenantId ?? '';

  const [config, setConfig] = useState<WabaConfig | null>(null);
  const [session, setSession] = useState<OAuthSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [connecting, setConnecting] = useState(false);
  const [oauthError, setOauthError] = useState<string | null>(null);

  const sessionToken = searchParams.get('session');
  const errorParam = searchParams.get('error');

  const loadConfig = useCallback(async () => {
    if (!tenantId) return;
    try {
      const cfg = await getWhatsAppConfig(tenantId);
      setConfig(cfg.status === 'CONNECTED' ? cfg : null);
    } catch {
      setConfig(null);
    } finally {
      setLoading(false);
    }
  }, [tenantId]);

  // Carrega sessió OAuth si venim del callback de Meta
  useEffect(() => {
    if (errorParam) {
      setOauthError(decodeURIComponent(errorParam));
      setLoading(false);
      return;
    }
    if (sessionToken) {
      getOAuthSession(sessionToken)
        .then(s => setSession(s))
        .catch(() => setOauthError('Sessió OAuth caducada. Torna a intentar-ho.'))
        .finally(() => setLoading(false));
    } else {
      loadConfig();
    }
  }, [sessionToken, errorParam, loadConfig]);

  const handleConnect = async () => {
    if (!tenantId) return;
    setConnecting(true);
    try {
      const result = await getConnectUrl(tenantId);
      window.location.href = result.oauthUrl;
    } catch (err: any) {
      toast('error', err?.body?.message || 'Error generant l\'URL d\'autenticació');
      setConnecting(false);
    }
  };

  const handleConfirm = async (wabaId: string, phoneNumberId: string) => {
    if (!sessionToken) return;
    try {
      await confirmConnection(sessionToken, wabaId, phoneNumberId);
      toast('success', 'WhatsApp Business connectat correctament');
      setSession(null);
      router.replace(window.location.pathname);
      await loadConfig();
    } catch (err: any) {
      toast('error', err?.body?.message || 'Error confirmant la connexió');
    }
  };

  const handleDisconnect = async () => {
    if (!tenantId) return;
    try {
      await disconnectWhatsApp(tenantId);
      toast('success', 'WhatsApp desconnectat');
      setConfig(null);
    } catch {
      toast('error', 'Error desconnectant WhatsApp');
    }
  };

  const handleCancelSession = () => {
    setSession(null);
    router.replace(window.location.pathname);
  };

  return (
    <PortalShell breadcrumb="admin · integrations · whatsapp">
      <div className="p-4 sm:p-8 space-y-6 max-w-3xl">

        {/* Header */}
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / integrations / whatsapp /</span>
          <div className="f-display font-bold text-xl mt-1">WhatsApp Business</div>
          <p className="text-sm text-ink-2 mt-1">
            Connecta el teu compte de WhatsApp Business (WABA) via Meta per enviar i rebre missatges des del portal.
          </p>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : oauthError ? (
          /* Error OAuth */
          <div className="amg-card card-clip p-5 space-y-4">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 bg-red-950/40 border border-red-500/30 flex items-center justify-center shrink-0">
                <IconSet.AlertCircle size={18} stroke="#f87171" />
              </div>
              <div>
                <div className="f-display font-bold text-sm">Error de connexió</div>
                <p className="text-sm text-ink-2 mt-1">{oauthError}</p>
              </div>
            </div>
            <AMGButton size="sm" icon={IconSet.Play} onClick={() => { setOauthError(null); router.replace(window.location.pathname); }}>
              Tornar a intentar
            </AMGButton>
          </div>
        ) : session ? (
          /* Selecció WABA */
          <WabaSelector session={session} onConfirm={handleConfirm} onCancel={handleCancelSession} />
        ) : config ? (
          /* Connectat */
          <ConnectedCard config={config} onDisconnect={handleDisconnect} />
        ) : (
          /* No connectat */
          <div className="amg-card card-clip p-5 space-y-4">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 bg-[#25D366]/10 border border-[#25D366]/30 flex items-center justify-center shrink-0">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="#25D366">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                </svg>
              </div>
              <div>
                <div className="f-display font-bold text-base">Connecta WhatsApp Business</div>
                <p className="text-sm text-ink-2 mt-1">
                  Autoritza l'accés al teu WABA via Meta OAuth. Seràs redirigit a Facebook per completar l'autorització.
                </p>
                <ul className="mt-3 space-y-1 text-sm text-ink-2">
                  {['Envia i rep missatges WhatsApp des del portal', 'L\'agent IA respon automàticament', 'Historial unificat a l\'Inbox'].map(t => (
                    <li key={t} className="flex items-center gap-2">
                      <IconSet.Check size={12} stroke="#4ade80" />
                      {t}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
            <AMGButton
              size="sm"
              onClick={handleConnect}
              disabled={connecting}
            >
              {connecting ? 'Redirigint a Meta…' : 'Connecta WhatsApp Business'}
            </AMGButton>
          </div>
        )}

        {/* Info */}
        <div className="amg-card card-clip p-4">
          <div className="f-mono text-label uppercase text-ink-3 mb-3">Sobre el flux OAuth</div>
          <div className="space-y-2 text-sm text-ink-2">
            <p>Meta factura directament al teu Facebook Business Manager. AMG no gestiona el consum de missatges Meta.</p>
            <p>El teu WABA i número han d'estar verificats a Meta Business Suite abans de connectar.</p>
          </div>
        </div>
      </div>
    </PortalShell>
  );
}

export default function WhatsAppIntegrationPage() {
  return (
    <Suspense fallback={
      <PortalShell breadcrumb="admin · integrations · whatsapp">
        <div className="flex justify-center py-12">
          <span className="w-5 h-5 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
        </div>
      </PortalShell>
    }>
      <WhatsAppIntegrationContent />
    </Suspense>
  );
}
