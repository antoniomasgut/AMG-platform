'use client';

import { useState, useEffect, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import {
  getGoogleStatus, getAuthUrl, updateModules, testConnection, disconnectTenant,
  getDriveFiles, getCalendarEvents, sendMail,
  type GoogleStatus, type DriveFileItem, type CalendarEventItem,
} from '@/services/google';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { AMGBadge } from '@/components/ui/badge';
import { IconSet } from '@/components/ui/icons';

type Tab = 'config' | 'drive' | 'calendar' | 'gmail' | 'sheets';

function fmtDate(d: string) {
  return new Date(d).toLocaleString('ca-ES', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}

function fmtSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

// ── Drive tab ──────────────────────────────────────────────────────────────────

function DriveTab({ tenantId }: { tenantId: string }) {
  const [folderId, setFolderId] = useState<string | undefined>(undefined);
  const [folderStack, setFolderStack] = useState<{ id: string; name: string }[]>([]);

  const { data: files = [], isLoading, refetch } = useQuery({
    queryKey: ['google-drive', tenantId, folderId],
    queryFn: () => getDriveFiles(tenantId, folderId),
    enabled: !!tenantId,
  });

  const enter = (file: DriveFileItem) => {
    setFolderStack(s => [...s, { id: file.id, name: file.name }]);
    setFolderId(file.id);
  };

  const goBack = () => {
    const next = folderStack.slice(0, -1);
    setFolderStack(next);
    setFolderId(next.length > 0 ? next[next.length - 1].id : undefined);
  };

  return (
    <div className="space-y-3">
      {/* Breadcrumb */}
      <div className="flex items-center gap-1.5 f-mono text-[10px] text-ink-3">
        <button onClick={() => { setFolderStack([]); setFolderId(undefined); }} className="hover:text-ink-0">
          Drive
        </button>
        {folderStack.map((f, i) => (
          <span key={f.id} className="flex items-center gap-1.5">
            <span>/</span>
            <button
              onClick={() => { setFolderStack(s => s.slice(0, i + 1)); setFolderId(f.id); }}
              className="hover:text-ink-0"
            >
              {f.name}
            </button>
          </span>
        ))}
      </div>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {folderStack.length > 0 && (
            <AMGButton size="sm" variant="ghost" onClick={goBack}>
              ← Enrere
            </AMGButton>
          )}
        </div>
        <AMGButton size="sm" variant="secondary" icon={IconSet.Refresh} onClick={() => refetch()}>
          Actualitzar
        </AMGButton>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8">
          <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
        </div>
      ) : files.length === 0 ? (
        <div className="p-6 text-center f-mono text-xs text-ink-3">Carpeta buida</div>
      ) : (
        <div className="amg-card card-clip divide-y divide-border-base">
          {files.map(f => (
            <div key={f.id} className="flex items-center gap-3 px-4 py-2.5 hover:bg-bg-1 transition-colors">
              <span className="text-ink-3 shrink-0">
                {f.mimeType === 'application/vnd.google-apps.folder'
                  ? <IconSet.Box size={14} />
                  : <IconSet.FileText size={14} />
                }
              </span>
              <div className="flex-1 min-w-0">
                {f.mimeType === 'application/vnd.google-apps.folder' ? (
                  <button
                    onClick={() => enter(f)}
                    className="f-mono text-xs text-ink-0 hover:text-accent-light truncate block text-left"
                  >
                    {f.name}
                  </button>
                ) : (
                  <a href={f.webViewLink} target="_blank" rel="noopener noreferrer"
                    className="f-mono text-xs text-ink-0 hover:text-accent-light truncate block">
                    {f.name}
                  </a>
                )}
                <div className="f-mono text-[10px] text-ink-3 mt-0.5">
                  {f.size > 0 ? fmtSize(f.size) : ''}{f.size > 0 && f.createdTime ? ' · ' : ''}{f.createdTime ? fmtDate(f.createdTime) : ''}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Calendar tab ───────────────────────────────────────────────────────────────

function CalendarTab({ tenantId }: { tenantId: string }) {
  const { data: events = [], isLoading, refetch } = useQuery({
    queryKey: ['google-calendar', tenantId],
    queryFn: () => getCalendarEvents(tenantId),
    enabled: !!tenantId,
  });

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <AMGButton size="sm" variant="secondary" icon={IconSet.Refresh} onClick={() => refetch()}>
          Actualitzar
        </AMGButton>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8">
          <span className="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin" />
        </div>
      ) : events.length === 0 ? (
        <div className="p-6 text-center f-mono text-xs text-ink-3">Cap event proper</div>
      ) : (
        <div className="amg-card card-clip divide-y divide-border-base">
          {events.map(ev => (
            <div key={ev.id} className="px-4 py-3 hover:bg-bg-1 transition-colors">
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <a href={ev.htmlLink} target="_blank" rel="noopener noreferrer"
                    className="f-display font-bold text-sm hover:text-accent-light truncate block">
                    {ev.summary}
                  </a>
                  {ev.description && (
                    <p className="f-mono text-[10px] text-ink-3 mt-0.5 line-clamp-2">{ev.description}</p>
                  )}
                </div>
                <div className="shrink-0 text-right">
                  <div className="f-mono text-[10px] text-ink-2">{fmtDate(ev.start)}</div>
                  {ev.end !== ev.start && (
                    <div className="f-mono text-[10px] text-ink-3">→ {fmtDate(ev.end)}</div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Gmail tab ──────────────────────────────────────────────────────────────────

function GmailTab({ tenantId }: { tenantId: string }) {
  const { toast } = useToast();
  const [to, setTo] = useState('');
  const [subject, setSubject] = useState('');
  const [body, setBody] = useState('');
  const [sending, setSending] = useState(false);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    setSending(true);
    try {
      const msg = await sendMail(tenantId, to, subject, body);
      toast('success', msg || 'Correu enviat');
      setTo(''); setSubject(''); setBody('');
    } catch (err: any) {
      toast('error', err?.body?.message || 'Error enviant el correu');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="amg-card card-clip p-5 space-y-4">
      <div className="f-mono text-label text-xs text-ink-3 uppercase tracking-widest">
        Envia un correu des del teu compte Gmail
      </div>
      <form onSubmit={handleSend} className="space-y-3">
        <div>
          <label className="f-mono text-[10px] text-ink-3 uppercase tracking-wider block mb-1">
            A (destinatari)
          </label>
          <input
            type="email"
            required
            value={to}
            onChange={e => setTo(e.target.value)}
            placeholder="client@exemple.com"
            className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
          />
        </div>
        <div>
          <label className="f-mono text-[10px] text-ink-3 uppercase tracking-wider block mb-1">
            Assumpte
          </label>
          <input
            type="text"
            required
            value={subject}
            onChange={e => setSubject(e.target.value)}
            placeholder="Assumpte del correu"
            className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
          />
        </div>
        <div>
          <label className="f-mono text-[10px] text-ink-3 uppercase tracking-wider block mb-1">
            Missatge
          </label>
          <textarea
            required
            value={body}
            onChange={e => setBody(e.target.value)}
            rows={6}
            placeholder="Escriu el missatge aquí..."
            className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 py-2 f-mono text-xs focus:outline-none focus:border-accent resize-none"
          />
        </div>
        <AMGButton type="submit" disabled={sending}>
          {sending ? 'Enviant…' : 'Enviar correu'}
        </AMGButton>
      </form>
    </div>
  );
}

// ── Sheets tab ─────────────────────────────────────────────────────────────────

function SheetsTab() {
  return (
    <div className="amg-card card-clip p-6 space-y-4">
      <div className="f-mono text-label text-xs text-ink-3 uppercase tracking-widest">
        Google Sheets
      </div>
      <div className="space-y-3">
        <p className="text-sm text-ink-2">
          Connecta fulls de càlcul de Google Sheets per llegir dades externes: catàlegs de productes, tarifes o configuracions editables pel tenant.
        </p>
        <div className="border border-border-base bg-bg-1 p-4 space-y-2">
          <div className="f-mono text-[10px] text-ink-3 uppercase tracking-wider">Casos d&apos;ús</div>
          <ul className="space-y-1">
            {[
              'Catàleg de productes o serveis',
              'Tarifes i preus editables sense codi',
              'Configuracions externes del negoci',
              'Llistes de clients o contactes',
            ].map(item => (
              <li key={item} className="flex items-start gap-2 f-mono text-xs text-ink-1">
                <span className="text-accent mt-0.5">▪</span>
                {item}
              </li>
            ))}
          </ul>
        </div>
        <div className="flex items-center gap-2 p-3 border border-border-base bg-bg-1">
          <AMGBadge tone="warning">Pròximament</AMGBadge>
          <span className="f-mono text-xs text-ink-2">
            La lectura de dades des de Sheets s&apos;activarà en una propera versió.
          </span>
        </div>
      </div>
    </div>
  );
}

// ── Main page ──────────────────────────────────────────────────────────────────

export default function GoogleIntegrationPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const tenantId = user?.tenantId ?? '';

  const [status, setStatus] = useState<GoogleStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [modules, setModules] = useState({ drive: false, gmail: false, calendar: false, sheets: false, business: false });
  const [driveFolderId, setDriveFolderId] = useState('');
  const [businessLocationId, setBusinessLocationId] = useState('');
  const [activeTab, setActiveTab] = useState<Tab>('config');

  const load = useCallback(async () => {
    if (!tenantId) return;
    setLoading(true);
    try {
      const s = await getGoogleStatus(tenantId);
      setStatus(s);
      setModules({ drive: s.driveEnabled, gmail: s.gmailEnabled, calendar: s.calendarEnabled, sheets: s.sheetsEnabled, business: s.businessEnabled });
      setDriveFolderId(s.driveFolderId ?? '');
      setBusinessLocationId(s.businessLocationId ?? '');
    } catch {
      setStatus(null);
    } finally {
      setLoading(false);
    }
  }, [tenantId]);

  useEffect(() => { load(); }, [load]);

  const handleConnect = async () => {
    if (!tenantId) return;
    const selected = Object.entries(modules).filter(([_, v]) => v).map(([k]) => k);
    if (selected.length === 0) { toast('warning', 'Selecciona almenys un mòdul'); return; }
    try {
      const redirectUri = window.location.origin + '/api/v1/google/callback';
      const result = await getAuthUrl(tenantId, selected, redirectUri);
      window.location.href = result.authUrl;
    } catch (err: any) {
      toast('error', err?.body?.message || 'Error generant URL');
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
        businessEnabled: modules.business,
        businessLocationId: businessLocationId || null,
        driveFolderId: driveFolderId || null,
      });
      toast('success', 'Configuració desada');
      load();
    } catch {
      toast('error', 'Error desant la configuració');
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
    if (!tenantId || !confirm('Desconnectar Google?')) return;
    try {
      await disconnectTenant(tenantId);
      toast('success', 'Google desconnectat');
      load();
    } catch {
      toast('error', 'Error desconnectant');
    }
  };

  const MODULE_LIST = [
    { key: 'drive' as const,    label: 'Google Drive',    desc: 'Desa documents al teu Drive' },
    { key: 'gmail' as const,    label: 'Gmail',           desc: 'Envia correus des del teu compte' },
    { key: 'calendar' as const, label: 'Google Calendar', desc: 'Crea events al teu calendari' },
    { key: 'sheets' as const,   label: 'Google Sheets',   desc: 'Llegeix dades dels teus fulls' },
    { key: 'business' as const, label: 'Google Business',  desc: 'Ressenyes: avís i resposta per Telegram' },
  ];

  const TABS: { id: Tab; label: string; disabled?: boolean }[] = [
    { id: 'config',   label: 'Configuració' },
    { id: 'drive',    label: 'Drive',    disabled: !status?.connected || !status?.driveEnabled },
    { id: 'gmail',    label: 'Gmail',    disabled: !status?.connected || !status?.gmailEnabled },
    { id: 'calendar', label: 'Calendar', disabled: !status?.connected || !status?.calendarEnabled },
    { id: 'sheets',   label: 'Sheets',   disabled: !status?.connected || !status?.sheetsEnabled },
  ];

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
            {/* Status header */}
            <div className="amg-card card-clip p-5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 bg-success/20 flex items-center justify-center">
                  <IconSet.Check size={16} className="text-success" />
                </div>
                <div>
                  <div className="f-display font-bold text-sm">Connectat</div>
                  <div className="f-mono text-xs text-ink-2">{status.email}</div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <AMGBadge tone="success">Actiu</AMGBadge>
                <AMGButton size="sm" variant="ghost" onClick={handleTest}>Test</AMGButton>
                <AMGButton size="sm" variant="danger" onClick={handleDisconnect}>Desconnecta</AMGButton>
              </div>
            </div>

            {/* Tabs */}
            <div className="flex gap-0 border-b border-border-base">
              {TABS.map(tab => (
                <button
                  key={tab.id}
                  disabled={tab.disabled}
                  onClick={() => !tab.disabled && setActiveTab(tab.id)}
                  className={`px-4 py-2 f-mono text-[11px] uppercase tracking-wider border-b-2 transition-colors ${
                    activeTab === tab.id
                      ? 'border-accent text-accent-light'
                      : tab.disabled
                      ? 'border-transparent text-ink-3 cursor-not-allowed'
                      : 'border-transparent text-ink-2 hover:text-ink-0'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            {/* Config tab */}
            {activeTab === 'config' && (
              <div className="space-y-4">
                <div className="amg-card card-clip p-5 space-y-4">
                  <div className="f-mono text-label text-xs text-ink-3 uppercase tracking-widest">Mòduls activats</div>
                  <div className="space-y-2">
                    {MODULE_LIST.map(({ key, label, desc }) => (
                      <label key={key} className="flex items-center gap-3 p-3 bg-bg-1 cursor-pointer hover:bg-bg-2 transition-colors">
                        <input
                          type="checkbox"
                          checked={modules[key]}
                          onChange={() => setModules(prev => ({ ...prev, [key]: !prev[key] }))}
                          className="w-4 h-4 accent-[#FF6B00]"
                        />
                        <div className="flex-1">
                          <div className="f-display font-bold text-sm">{label}</div>
                          <div className="f-mono text-[10px] text-ink-3">{desc}</div>
                        </div>
                        {status[`${key}Enabled` as keyof GoogleStatus] && (
                          <AMGBadge tone="success">Actiu</AMGBadge>
                        )}
                      </label>
                    ))}
                  </div>

                  {/* Drive folder ID */}
                  {modules.drive && (
                    <div>
                      <label className="f-mono text-[10px] text-ink-3 uppercase tracking-wider block mb-1">
                        Drive Folder ID <span className="normal-case text-ink-3">(opcional — carpeta arrel per documents del tenant)</span>
                      </label>
                      <input
                        type="text"
                        value={driveFolderId}
                        onChange={e => setDriveFolderId(e.target.value)}
                        placeholder="p.ex. 1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms"
                        className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                      />
                      <p className="f-mono text-[10px] text-ink-3 mt-1">
                        L&apos;ID es troba a la URL de Google Drive: drive.google.com/drive/folders/<span className="text-ink-1">ID</span>
                      </p>
                    </div>
                  )}

                  {/* Google Business location ID */}
                  {modules.business && (
                    <div>
                      <label className="f-mono text-[10px] text-ink-3 uppercase tracking-wider block mb-1">
                        Business Location ID <span className="normal-case text-ink-3">(ID de la fitxa de Google Business)</span>
                      </label>
                      <input
                        type="text"
                        value={businessLocationId}
                        onChange={e => setBusinessLocationId(e.target.value)}
                        placeholder="p.ex. 12345678901234567890 o accounts/123/locations/456"
                        className="w-full bg-bg-1 border border-border-base text-ink-0 px-3 h-9 f-mono text-xs focus:outline-none focus:border-accent"
                      />
                      <p className="f-mono text-[10px] text-ink-3 mt-1">
                        Necessari per sincronitzar ressenyes. El tenant rebrà un avís per Telegram de cada ressenya nova i podrà respondre-la des d&apos;allà.
                      </p>
                    </div>
                  )}

                  <AMGButton onClick={handleSaveModules}>Desar configuració</AMGButton>
                </div>
              </div>
            )}

            {/* Drive tab */}
            {activeTab === 'drive' && <DriveTab tenantId={tenantId} />}

            {/* Gmail tab */}
            {activeTab === 'gmail' && <GmailTab tenantId={tenantId} />}

            {/* Calendar tab */}
            {activeTab === 'calendar' && <CalendarTab tenantId={tenantId} />}

            {/* Sheets tab */}
            {activeTab === 'sheets' && <SheetsTab />}
          </>
        ) : (
          /* Not connected */
          <div className="amg-card card-clip p-6 space-y-4">
            <div className="text-center py-4">
              <div className="w-12 h-12 bg-ink-3/10 flex items-center justify-center mx-auto mb-3">
                <IconSet.Globe size={24} className="text-ink-3" />
              </div>
              <div className="f-display font-bold text-sm mb-1">Google no connectat</div>
              <p className="text-sm text-ink-2 mb-4">Selecciona els mòduls i connecta el teu compte</p>
            </div>

            <div className="space-y-2">
              {MODULE_LIST.map(({ key, label, desc }) => (
                <label key={key} className="flex items-center gap-3 p-3 bg-bg-1 cursor-pointer hover:bg-bg-2 transition-colors">
                  <input
                    type="checkbox"
                    checked={modules[key]}
                    onChange={() => setModules(prev => ({ ...prev, [key]: !prev[key] }))}
                    className="w-4 h-4 accent-[#FF6B00]"
                  />
                  <div className="flex-1">
                    <div className="f-display font-bold text-sm">{label}</div>
                    <div className="f-mono text-[10px] text-ink-3">{desc}</div>
                  </div>
                </label>
              ))}
            </div>

            <div className="flex justify-center pt-2">
              <AMGButton icon={IconSet.Play} onClick={handleConnect}>
                Connecta amb Google
              </AMGButton>
            </div>
          </div>
        )}
      </div>
    </PortalShell>
  );
}
