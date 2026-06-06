'use client';

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getGoogleStatus, getAuthUrl, updateModules, testConnection, disconnectTenant,
  type GoogleStatus,
} from '@/services/google';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { I } from '@/components/ui/icons';

export default function GoogleIntegrationPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const tenantId = user?.tenantId ?? '';

  const [status, setStatus] = useState<GoogleStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [modules, setModules] = useState({ drive: false, gmail: false, calendar: false, sheets: false });

  const load = useCallback(async () => {
    if (!tenantId) return;
    setLoading(true);
    try {
      const s = await getGoogleStatus(tenantId);
      setStatus(s);
      setModules({ drive: s.driveEnabled, gmail: s.gmailEnabled, calendar: s.calendarEnabled, sheets: s.sheetsEnabled });
    } catch {
      // not connected
      setStatus(null);
    } finally {
      setLoading(false);
    }
  }, [tenantId]);

  useEffect(() => { load(); }, [load]);

  const handleConnect = async () => {
    if (!tenantId) return;
    const selected = Object.entries(modules).filter(([_, v]) => v).map(([k]) => k);
    if (selected.length === 0) {
      toast('warning', 'Selecciona almenys un mòdul');
      return;
    }
    try {
      const redirectUri = window.location.origin + '/api/v1/google/callback';
      const result = await getAuthUrl(tenantId, selected, redirectUri);
      window.location.href = result.authUrl;
    } catch (err: any) {
      toast('error', err?.body?.message || 'Error generant l\'URL d\'autenticació');
    }
  };

  const handleSaveModules = async () => {
    if (!tenantId || !status?.connected) return;
    try {
      await updateModules(tenantId, {
        driveEnabled: modules.drive,
        gmailEnabled: modules.gmail,
        calendarEnabled: modules.calendar,
        sheetsEnabled: modules.sheets,
      });
      toast('success', 'Mòduls actualitzats');
      load();
    } catch {
      toast('error', 'Error actualitzant mòduls');
    }
  };

  const handleTest = async () => {
    if (!tenantId) return;
    try {
      const result = await testConnection(tenantId);
      toast('success', result);
    } catch (err: any) {
      toast('error', err?.body?.message || 'Error de connexió');
    }
  };

  const handleDisconnect = async () => {
    if (!tenantId) return;
    try {
      await disconnectTenant(tenantId);
      toast('success', 'Google desconnectat');
      load();
    } catch {
      toast('error', 'Error desconnectant');
    }
  };

  const toggleModule = (key: keyof typeof modules) => {
    setModules(prev => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <PortalShell breadcrumb="admin · integrations · google">
      <div className="p-4 sm:p-8 space-y-6 max-w-3xl">
        <div>
          <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / admin / integrations / google /</span>
          <div className="f-display font-bold text-xl mt-1">Google Workspace</div>
          <p className="text-sm text-ink-2 mt-1">Connecta el teu compte de Google per utilitzar Drive, Gmail, Calendar i Sheets</p>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : status?.connected ? (
          <>
            {/* Connected status */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-green-500/20 rounded-full flex items-center justify-center">
                    <I.Check size={18} className="text-green-500" />
                  </div>
                  <div>
                    <div className="f-display font-bold text-sm">Connectat</div>
                    <div className="f-mono text-xs text-ink-2">{status.email}</div>
                  </div>
                </div>
                <AMGBadge tone="success">Actiu</AMGBadge>
              </div>
            </div>

            {/* Module toggles */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div className="f-display font-bold text-sm">Mòduls activats</div>
              <div className="space-y-3">
                {([
                  { key: 'drive' as const, label: 'Google Drive', desc: 'Desa documents al teu Drive' },
                  { key: 'gmail' as const, label: 'Gmail', desc: 'Envia correus des del teu compte' },
                  { key: 'calendar' as const, label: 'Google Calendar', desc: 'Crea events al teu calendari' },
                  { key: 'sheets' as const, label: 'Google Sheets', desc: 'Llegeix dades dels teus fulls' },
                ]).map(({ key, label, desc }) => (
                  <label key={key} className="flex items-center gap-3 p-3 bg-[#0d0d1a] rounded cursor-pointer">
                    <input
                      type="checkbox"
                      checked={modules[key]}
                      onChange={() => toggleModule(key)}
                      className="w-4 h-4 accent-[#FF6B00]"
                    />
                    <div className="flex-1">
                      <div className="text-sm font-medium">{label}</div>
                      <div className="f-mono text-[10px] text-ink-3">{desc}</div>
                    </div>
                  </label>
                ))}
              </div>
              <AMGButton size="sm" onClick={handleSaveModules}>Desar configuració</AMGButton>
            </div>

            {/* Actions */}
            <div className="flex gap-3">
              <AMGButton variant="secondary" onClick={handleTest}>
                Prova connexió
              </AMGButton>
              <AMGButton variant="danger" onClick={handleDisconnect}>
                Desconnecta
              </AMGButton>
            </div>
          </>
        ) : (
          <>
            {/* Not connected */}
            <div className="amg-card card-clip p-6 space-y-4">
              <div className="text-center py-4">
                <I.Play size={32} stroke="#64748b" className="mx-auto mb-3" />
                <div className="f-display font-bold text-sm mb-1">Google no connectat</div>
                <p className="text-sm text-ink-2 mb-4">Selecciona els mòduls que vols utilitzar i connecta el teu compte</p>
              </div>

              <div className="space-y-3">
                {([
                  { key: 'drive' as const, label: 'Google Drive', desc: 'Desa documents al teu Drive' },
                  { key: 'gmail' as const, label: 'Gmail', desc: 'Envia correus des del teu compte' },
                  { key: 'calendar' as const, label: 'Google Calendar', desc: 'Crea events al teu calendari' },
                  { key: 'sheets' as const, label: 'Google Sheets', desc: 'Llegeix dades dels teus fulls' },
                ]).map(({ key, label, desc }) => (
                  <label key={key} className="flex items-center gap-3 p-3 bg-[#0d0d1a] rounded cursor-pointer">
                    <input
                      type="checkbox"
                      checked={modules[key]}
                      onChange={() => toggleModule(key)}
                      className="w-4 h-4 accent-[#FF6B00]"
                    />
                    <div className="flex-1">
                      <div className="text-sm font-medium">{label}</div>
                      <div className="f-mono text-[10px] text-ink-3">{desc}</div>
                    </div>
                  </label>
                ))}
              </div>

              <div className="flex justify-center pt-2">
                <AMGButton icon={I.Play} onClick={handleConnect}>
                  Connecta amb Google
                </AMGButton>
              </div>
            </div>
          </>
        )}
      </div>
    </PortalShell>
  );
}
