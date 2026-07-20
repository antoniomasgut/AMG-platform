'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { useToast } from '@/lib/toast-context';
import { getTenant } from '@/services/admin';
import {
  listSocialPosts,
  cancelSocialPost,
  syncSocialMetrics,
  NETWORK_LABELS,
  NETWORK_ICONS,
  POST_TYPE_LABELS,
  STATUS_LABELS,
  STATUS_COLORS,
  type SocialPost,
} from '@/services/social';

function fmtDate(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleString('ca-ES', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

export default function SocialPostsPage() {
  const { id } = useParams<{ id: string }>();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [networkFilter, setNetworkFilter] = useState<string>('ALL');

  const { data: tenant } = useQuery({
    queryKey: ['tenant', id],
    queryFn: () => getTenant(id),
  });

  const { data: posts = [], isLoading } = useQuery<SocialPost[]>({
    queryKey: ['social-posts', id],
    queryFn: () => listSocialPosts(id),
    refetchInterval: 30_000,
  });

  const cancelPost = useMutation({
    mutationFn: (postId: string) => cancelSocialPost(id, postId),
    onSuccess: () => {
      toast('success', 'Post cancel·lat');
      qc.invalidateQueries({ queryKey: ['social-posts', id] });
    },
    onError: (e: Error) => toast('error', e.message),
  });

  const syncMetrics = useMutation({
    mutationFn: () => syncSocialMetrics(id),
    onSuccess: () => {
      toast('success', 'Mètriques sincronitzades');
      qc.invalidateQueries({ queryKey: ['social-posts', id] });
    },
    onError: (e: Error) => toast('error', e.message),
  });

  const tenantName = tenant?.name ?? id;
  const filtered = networkFilter === 'ALL'
    ? posts
    : posts.filter(p => p.network === networkFilter);

  const networks = ['ALL', 'INSTAGRAM', 'FACEBOOK', 'GOOGLE_BUSINESS'];

  return (
    <PortalShell breadcrumb={`Tenants / ${tenantName} / Social / Posts`}>
      <div className="space-y-6">

        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Historial de posts</h1>
            <p className="text-sm text-gray-500 mt-1">{tenantName}</p>
          </div>
          <div className="flex gap-2">
            <AMGButton
              variant="secondary"
              onClick={() => syncMetrics.mutate()}
              disabled={syncMetrics.isPending}
            >
              {syncMetrics.isPending ? 'Sincronitzant…' : '🔄 Mètriques'}
            </AMGButton>
            <Link href={`/portal/admin/tenants/${id}/social`}>
              <AMGButton variant="secondary">← Connexió</AMGButton>
            </Link>
          </div>
        </div>

        {/* Filtres per xarxa */}
        <div className="flex gap-2 flex-wrap">
          {networks.map(n => (
            <button
              key={n}
              onClick={() => setNetworkFilter(n)}
              className={`px-3 py-1 rounded-full text-sm border transition-colors ${
                networkFilter === n
                  ? 'bg-gray-900 text-white border-gray-900'
                  : 'bg-white text-gray-600 border-gray-200 hover:border-gray-400'
              }`}
            >
              {n === 'ALL' ? 'Totes' : `${NETWORK_ICONS[n]} ${NETWORK_LABELS[n]}`}
            </button>
          ))}
        </div>

        {/* Stats ràpids per estat */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {(['PUBLISHED', 'SCHEDULED', 'FAILED', 'CANCELLED'] as const).map(status => {
            const count = posts.filter(p => p.status === status).length;
            return (
              <div key={status} className="bg-white rounded-xl border border-gray-200 p-4 text-center">
                <div className="text-2xl font-bold">{count}</div>
                <div className="text-xs text-gray-500 mt-1">{STATUS_LABELS[status]}</div>
              </div>
            );
          })}
        </div>

        {/* Mètriques d'engagement agregades (P27) */}
        {posts.some(p => p.status === 'PUBLISHED' && (p.reach != null || p.likes != null)) && (
          <div className="grid grid-cols-3 gap-4">
            {[
              {
                icon: '👁',
                label: 'Abast total',
                value: posts.reduce((s, p) => s + (p.reach ?? 0), 0).toLocaleString('ca-ES'),
              },
              {
                icon: '❤️',
                label: 'Likes totals',
                value: posts.reduce((s, p) => s + (p.likes ?? 0), 0).toLocaleString('ca-ES'),
              },
              {
                icon: '💬',
                label: 'Comentaris',
                value: posts.reduce((s, p) => s + (p.comments ?? 0), 0).toLocaleString('ca-ES'),
              },
            ].map(({ icon, label, value }) => (
              <div key={label} className="bg-blue-50 border border-blue-100 rounded-xl p-4 text-center">
                <div className="text-xl font-bold text-blue-800">{icon} {value}</div>
                <div className="text-xs text-blue-600 mt-1">{label}</div>
              </div>
            ))}
          </div>
        )}

        {/* Llista de posts */}
        {isLoading ? (
          <div className="text-center py-12 text-gray-400">Carregant…</div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <p className="text-lg">Cap post publicat encara</p>
            <p className="text-sm mt-1">
              El tenant pot escriure <code>/publica</code> al Telegram per iniciar el flux.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filtered.map(post => (
              <div
                key={post.id}
                className="bg-white rounded-xl border border-gray-200 p-5 space-y-3"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <span className="text-2xl">{NETWORK_ICONS[post.network] ?? '📣'}</span>
                    <div>
                      <div className="font-medium">
                        {NETWORK_LABELS[post.network]} — {POST_TYPE_LABELS[post.postType] ?? post.postType}
                      </div>
                      <div className="text-xs text-gray-400 mt-0.5">
                        {post.publishedAt
                          ? `Publicat ${fmtDate(post.publishedAt)}`
                          : post.scheduledAt
                            ? `Programat per ${fmtDate(post.scheduledAt)}`
                            : `Creat ${fmtDate(post.createdAt)}`}
                      </div>
                    </div>
                  </div>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[post.status]}`}>
                    {STATUS_LABELS[post.status]}
                  </span>
                </div>

                {post.caption && (
                  <p className="text-sm text-gray-700 line-clamp-3 bg-gray-50 rounded-lg p-3">
                    {post.caption}
                  </p>
                )}

                {post.mediaUrl && (
                  <p className="text-xs text-gray-500">
                    Imatge:{' '}
                    <a href={post.mediaUrl} target="_blank" rel="noreferrer"
                      className="underline text-blue-600 break-all">
                      {post.mediaUrl}
                    </a>
                  </p>
                )}

                {/* Mètriques d'engagement (sincronitzades diàriament) */}
                {post.status === 'PUBLISHED' && (post.reach != null || post.likes != null || post.comments != null) && (
                  <div className="flex gap-4 text-sm text-gray-600 bg-gray-50 rounded-lg px-3 py-2">
                    {post.reach != null && (
                      <span title="Abast">👁 <b>{post.reach.toLocaleString()}</b></span>
                    )}
                    {post.likes != null && (
                      <span title="Likes">❤️ <b>{post.likes.toLocaleString()}</b></span>
                    )}
                    {post.comments != null && (
                      <span title="Comentaris">💬 <b>{post.comments.toLocaleString()}</b></span>
                    )}
                    {post.metricsSyncedAt && (
                      <span className="ml-auto text-xs text-gray-400">
                        actualitzat {fmtDate(post.metricsSyncedAt)}
                      </span>
                    )}
                  </div>
                )}

                {/* Link directe al post publicat */}
                {post.externalPostUrl ? (
                  <a
                    href={post.externalPostUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-blue-600 underline"
                  >
                    🔗 Veure post
                  </a>
                ) : post.externalPostId ? (
                  <p className="text-xs text-gray-400">
                    ID extern: <code>{post.externalPostId}</code>
                  </p>
                ) : null}

                {post.errorMessage && (
                  <p className="text-xs text-red-600 bg-red-50 rounded p-2">
                    ⚠ {post.errorMessage}
                  </p>
                )}

                {post.status === 'SCHEDULED' && (
                  <div className="flex justify-end">
                    <AMGButton
                      variant="secondary"
                      onClick={() => cancelPost.mutate(post.id)}
                      disabled={cancelPost.isPending}
                    >
                      Cancel·lar
                    </AMGButton>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

      </div>
    </PortalShell>
  );
}
