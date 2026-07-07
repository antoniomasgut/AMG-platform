'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { useToast } from '@/lib/toast-context';
import { getTenant } from '@/services/admin';
import {
  getMetaStatus,
  saveMetaConfig,
  getSocialFeatures,
  updateSocialFeatures,
  type MetaConfigRequest,
  type SocialFeatures,
} from '@/services/social';

const FEATURE_LIST: { key: keyof SocialFeatures; label: string; desc: string }[] = [
  { key: 'aiSuggestions',      label: 'Suggeriments IA setmanals',   desc: 'Cada dilluns, una idea de post al Telegram del tenant' },
  { key: 'autoPostReviews',    label: 'Compartir ressenyes 5★',      desc: 'Botó per publicar les ressenyes de 5 estrelles a xarxes' },
  { key: 'weeklyAnalytics',    label: 'Resum setmanal d\'analítiques', desc: 'Abast, likes i comentaris dels posts publicats' },
  { key: 'commentsToTelegram', label: 'Comentaris → Telegram',       desc: 'Rep i respon comentaris de xarxes des de Telegram' },
  { key: 'dmsToInbox',         label: 'DMs IG/Messenger → Inbox',    desc: 'Els missatges directes arriben a l\'Inbox; l\'IA prepara la resposta i tu l\'aproves per Telegram' },
];

export default function SocialConnectionPage() {
  const { id } = useParams<{ id: string }>();
  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: tenant } = useQuery({
    queryKey: ['tenant', id],
    queryFn: () => getTenant(id),
  });

  const { data: metaStatus } = useQuery({
    queryKey: ['social-meta-status', id],
    queryFn: () => getMetaStatus(id),
  });

  const [form, setForm] = useState<MetaConfigRequest>({
    facebookPageId:     '',
    instagramAccountId: '',
    pageAccessToken:    '',
    pagesManagedPosts:  true,
    igContentPublish:   true,
  });

  const saveMeta = useMutation({
    mutationFn: () => saveMetaConfig(id, form),
    onSuccess: () => {
      toast('success', 'Configuració Meta desada');
      qc.invalidateQueries({ queryKey: ['social-meta-status', id] });
      setForm(f => ({ ...f, pageAccessToken: '' }));
    },
    onError: (e: Error) => toast('error', e.message),
  });

  const { data: features } = useQuery({
    queryKey: ['social-features', id],
    queryFn: () => getSocialFeatures(id),
  });

  const toggleFeature = useMutation({
    mutationFn: (next: SocialFeatures) => updateSocialFeatures(id, next),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['social-features', id] });
    },
    onError: (e: Error) => toast('error', e.message),
  });

  const onToggle = (key: keyof SocialFeatures, value: boolean) => {
    if (!features) return;
    toggleFeature.mutate({
      commentsToTelegram: features.commentsToTelegram,
      weeklyAnalytics:    features.weeklyAnalytics,
      aiSuggestions:      features.aiSuggestions,
      autoPostReviews:    features.autoPostReviews,
      dmsToInbox:         features.dmsToInbox,
      [key]: value,
    });
  };

  const tenantName = tenant?.name ?? id;

  return (
    <PortalShell breadcrumb={`Tenants / ${tenantName} / Social Publisher`}>
      <div className="max-w-2xl mx-auto space-y-8">

        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Social Publisher</h1>
            <p className="text-sm text-gray-500 mt-1">
              Connexió de xarxes socials per a <strong>{tenantName}</strong>
            </p>
          </div>
          <Link href={`/portal/admin/tenants/${id}/social/posts`}>
            <AMGButton variant="secondary">Historial de posts →</AMGButton>
          </Link>
        </div>

        {/* Estat connexió Meta */}
        <section className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
          <h2 className="font-semibold text-lg">Meta (Instagram + Facebook)</h2>

          {metaStatus?.connected ? (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-green-500 inline-block" />
                <span className="text-sm font-medium text-green-700">Connectat</span>
                {metaStatus.tokenValid === false && (
                  <span className="text-xs text-red-600 ml-2">⚠ Token caducat</span>
                )}
              </div>
              {metaStatus.facebookPageId && (
                <p className="text-sm text-gray-600">
                  Facebook Page ID: <code className="bg-gray-100 px-1 rounded">{metaStatus.facebookPageId}</code>
                </p>
              )}
              {metaStatus.instagramAccountId && (
                <p className="text-sm text-gray-600">
                  Instagram Account ID: <code className="bg-gray-100 px-1 rounded">{metaStatus.instagramAccountId}</code>
                </p>
              )}
              <div className="flex gap-4 text-sm">
                <span className={metaStatus.pagesManagedPosts ? 'text-green-600' : 'text-gray-400'}>
                  {metaStatus.pagesManagedPosts ? '✅' : '❌'} pages_manage_posts
                </span>
                <span className={metaStatus.igContentPublish ? 'text-green-600' : 'text-gray-400'}>
                  {metaStatus.igContentPublish ? '✅' : '❌'} instagram_content_publish
                </span>
              </div>
              {metaStatus.tokenExpiresAt && (
                <p className="text-xs text-gray-400">
                  Token caduca: {new Date(metaStatus.tokenExpiresAt).toLocaleDateString('ca-ES')}
                </p>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-gray-300 inline-block" />
              <span className="text-sm text-gray-500">No connectat</span>
            </div>
          )}

          {/* Formulari configuració */}
          <div className="border-t border-gray-100 pt-4 space-y-3">
            <p className="text-sm font-medium text-gray-700">
              {metaStatus?.connected ? 'Actualitzar configuració' : 'Configurar'}
            </p>

            <div>
              <label className="block text-xs text-gray-500 mb-1">Facebook Page ID</label>
              <input
                type="text"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
                placeholder="Ex: 123456789"
                value={form.facebookPageId}
                onChange={e => setForm(f => ({ ...f, facebookPageId: e.target.value }))}
              />
            </div>

            <div>
              <label className="block text-xs text-gray-500 mb-1">Instagram Account ID</label>
              <input
                type="text"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
                placeholder="Ex: 987654321"
                value={form.instagramAccountId}
                onChange={e => setForm(f => ({ ...f, instagramAccountId: e.target.value }))}
              />
            </div>

            <div>
              <label className="block text-xs text-gray-500 mb-1">
                Page Access Token (Long-lived, 60 dies)
              </label>
              <input
                type="password"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm font-mono"
                placeholder={metaStatus?.connected ? '••••• (deixa buit per mantenir)' : 'Enganxa el token'}
                value={form.pageAccessToken}
                onChange={e => setForm(f => ({ ...f, pageAccessToken: e.target.value }))}
              />
            </div>

            <div className="flex gap-4">
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.pagesManagedPosts}
                  onChange={e => setForm(f => ({ ...f, pagesManagedPosts: e.target.checked }))}
                />
                pages_manage_posts
              </label>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.igContentPublish}
                  onChange={e => setForm(f => ({ ...f, igContentPublish: e.target.checked }))}
                />
                instagram_content_publish
              </label>
            </div>

            <AMGButton
              onClick={() => saveMeta.mutate()}
              disabled={saveMeta.isPending}
            >
              {saveMeta.isPending ? 'Desant…' : 'Desar configuració Meta'}
            </AMGButton>
          </div>
        </section>

        {/* Extensions socials (Mòdul 55) */}
        <section className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
          <div>
            <h2 className="font-semibold text-lg">Funcionalitats socials</h2>
            <p className="text-sm text-gray-500 mt-1">
              Activa o desactiva cada extensió per a aquest tenant.
            </p>
          </div>

          {!features?.enabled ? (
            <p className="text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
              El servei Social Publisher no està actiu per a aquest tenant. Activa'l primer per gestionar aquestes funcionalitats.
            </p>
          ) : (
            <div className="divide-y divide-gray-100">
              {FEATURE_LIST.map(({ key, label, desc }) => (
                <div key={key} className="flex items-center justify-between py-3 gap-4">
                  <div>
                    <p className="text-sm font-medium text-gray-800">{label}</p>
                    <p className="text-xs text-gray-500">{desc}</p>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer shrink-0">
                    <input
                      type="checkbox"
                      className="sr-only peer"
                      checked={features[key]}
                      disabled={toggleFeature.isPending}
                      onChange={e => onToggle(key, e.target.checked)}
                    />
                    <div className="w-11 h-6 bg-gray-200 rounded-full peer peer-checked:bg-green-500 peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all" />
                  </label>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Google Business */}
        <section className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="font-semibold text-lg mb-2">Google Business Profile</h2>
          <p className="text-sm text-gray-500">
            La connexió GBP usa les credencials OAuth de Google Workspace del tenant.
            Configura-la a <Link href={`/portal/admin/tenants/${id}`} className="underline text-blue-600">Detall del tenant → Google</Link>.
          </p>
          <p className="text-xs text-gray-400 mt-2">
            Cal que <code>business_enabled = true</code> i <code>business_location_id</code> estiguin configurats.
          </p>
        </section>

        {/* Instruccions Telegram */}
        <section className="bg-amber-50 border border-amber-200 rounded-xl p-6 space-y-2">
          <h2 className="font-semibold text-amber-800">Flux de publicació via Telegram</h2>
          <p className="text-sm text-amber-700">
            El tenant pot escriure <code className="bg-amber-100 px-1 rounded">/publica</code> al seu
            xat de Telegram per iniciar el flux interactiu de publicació multi-xarxa.
          </p>
          <ul className="text-sm text-amber-700 list-disc pl-5 space-y-1">
            <li>Selecciona les xarxes (I / F / G)</li>
            <li>Tria el tipus (FOTO / TEXT / NOTICIES / OFERTA / EVENT)</li>
            <li>Proporciona la URL de la imatge (opcional)</li>
            <li>Escriu el caption o demana-li a la IA</li>
            <li>Confirma amb SI per publicar</li>
          </ul>
        </section>

      </div>
    </PortalShell>
  );
}
