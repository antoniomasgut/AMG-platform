'use client';

import { useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import Image from 'next/image';
import {
  listAssets, uploadAsset, deleteAsset, getAssetStats,
  getAssetCategory, type AssetResponse, type AssetCategory,
} from '@/services/assets';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { IconSet } from '@/components/ui/icons';

function fmtSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

function FileIcon({ mimeType }: { mimeType: string }) {
  if (mimeType === 'application/pdf') {
    return <IconSet.FileText size={32} stroke="#ef4444" />;
  }
  if (
    mimeType === 'application/msword' ||
    mimeType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ) {
    return <IconSet.FileText size={32} stroke="#3b82f6" />;
  }
  if (
    mimeType === 'application/vnd.ms-excel' ||
    mimeType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ) {
    return <IconSet.FileText size={32} stroke="#22c55e" />;
  }
  return <IconSet.Box size={32} stroke="#64748b" />;
}

const FILTER_LABELS: Record<AssetCategory, string> = {
  all: 'Tots',
  images: 'Imatges',
  documents: 'Documents',
  other: 'Altres',
};

export default function AssetsPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [filter, setFilter] = useState<AssetCategory>('all');

  const tenantId = user?.tenantId ?? '';

  const { data: assets = [], isLoading } = useQuery({
    queryKey: ['assets', tenantId],
    queryFn: () => listAssets(tenantId),
    enabled: !!tenantId,
  });

  const { data: stats } = useQuery({
    queryKey: ['asset-stats', tenantId],
    queryFn: () => getAssetStats(tenantId),
    enabled: !!tenantId,
  });

  const { mutate: doUpload, isPending: uploading } = useMutation({
    mutationFn: (file: File) => uploadAsset(tenantId, file),
    onSuccess: () => {
      toast('success', 'Fitxer pujat correctament');
      qc.invalidateQueries({ queryKey: ['assets', tenantId] });
      qc.invalidateQueries({ queryKey: ['asset-stats', tenantId] });
    },
    onError: () => toast('error', "Error pujant el fitxer"),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: (assetId: string) => deleteAsset(assetId),
    onSuccess: () => {
      toast('success', 'Fitxer eliminat');
      qc.invalidateQueries({ queryKey: ['assets', tenantId] });
      qc.invalidateQueries({ queryKey: ['asset-stats', tenantId] });
    },
    onError: () => toast('error', "Error eliminant el fitxer"),
  });

  const handleFiles = (files: FileList | null) => {
    if (!files) return;
    Array.from(files).forEach((file) => doUpload(file));
  };

  const copyUrl = (url: string) => {
    navigator.clipboard.writeText(window.location.origin + url);
    toast('success', 'URL copiada al porta-retalls');
  };

  const filtered = (assets as AssetResponse[]).filter(
    (a) => filter === 'all' || getAssetCategory(a.mimeType) === filter
  );

  const usedPct = stats ? Math.min(100, (stats.usedBytes / stats.quotaBytes) * 100) : 0;
  const quotaColor = usedPct > 90 ? 'bg-danger' : usedPct > 70 ? 'bg-warning' : 'bg-accent';

  if (!user) return null;

  const isImage = (mime: string) => mime.startsWith('image/');

  return (
    <PortalShell breadcrumb="assets">
      <div className="p-4 sm:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <span className="f-mono text-label uppercase text-accent-light tracking-widest">/ portal / assets /</span>
            <div className="f-display font-bold text-xl mt-1">Fitxers i Imatges</div>
          </div>
          <AMGButton icon={IconSet.Upload} loading={uploading} onClick={() => inputRef.current?.click()}>
            Pujar fitxer
          </AMGButton>
          <input
            ref={inputRef}
            type="file"
            multiple
            accept="image/*,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            className="hidden"
            onChange={(e) => handleFiles(e.target.files)}
          />
        </div>

        {/* Quota bar */}
        {stats && (
          <div className="amg-card card-clip p-4 space-y-2">
            <div className="flex items-center justify-between">
              <span className="f-mono text-[10px] uppercase text-ink-2">Emmagatzematge</span>
              <span className="f-mono text-xs text-ink-1">
                {fmtSize(stats.usedBytes)} / {fmtSize(stats.quotaBytes)}
                <span className="text-ink-3 ml-2">· {stats.fileCount} fitxers</span>
              </span>
            </div>
            <div className="h-1.5 bg-[rgba(255,255,255,0.06)] rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full transition-all ${quotaColor}`}
                style={{ width: `${usedPct}%` }}
              />
            </div>
          </div>
        )}

        {/* Drop zone */}
        <div
          onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => { e.preventDefault(); setDragging(false); handleFiles(e.dataTransfer.files); }}
          className={`amg-card card-clip border-2 border-dashed p-10 text-center transition-colors ${
            dragging ? 'border-accent bg-accent-muted' : 'border-border-base'
          }`}
        >
          {uploading ? (
            <div className="flex flex-col items-center gap-3">
              <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
              <span className="f-mono text-label text-ink-2">Pujant...</span>
            </div>
          ) : (
            <>
              <IconSet.Upload size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Arrossega fitxers aquí</div>
              <p className="f-mono text-label text-ink-2">Imatges, PDF, Word, Excel · o fes clic a &quot;Pujar fitxer&quot;</p>
            </>
          )}
        </div>

        {/* Filter tabs */}
        {assets.length > 0 && (
          <div className="flex gap-1">
            {(['all', 'images', 'documents', 'other'] as AssetCategory[]).map((cat) => (
              <button
                key={cat}
                onClick={() => setFilter(cat)}
                className={`px-3 py-1.5 text-[10px] uppercase f-mono rounded transition-colors ${
                  filter === cat
                    ? 'bg-[rgba(255,107,0,0.12)] text-accent border border-accent/30'
                    : 'text-ink-2 hover:text-ink-1 border border-transparent'
                }`}
              >
                {FILTER_LABELS[cat]}
                {cat === 'all' && <span className="ml-1 text-ink-3">({assets.length})</span>}
              </button>
            ))}
          </div>
        )}

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="amg-card card-clip p-8 text-center">
            <IconSet.Image size={28} stroke="#64748b" className="mx-auto mb-3" />
            <div className="f-display font-bold text-sm mb-1">
              {assets.length === 0 ? 'Cap fitxer' : 'Cap fitxer en aquesta categoria'}
            </div>
            <p className="f-mono text-label text-ink-2">
              {assets.length === 0 ? 'Puja el primer fitxer del teu tenant' : 'Canvia el filtre per veure altres fitxers'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
            {filtered.map((asset) => (
              <div key={asset.id} className="amg-card card-clip overflow-hidden group">
                {/* Miniatura */}
                <div className="aspect-square bg-bg-1 relative flex items-center justify-center">
                  {isImage(asset.mimeType) ? (
                    <Image
                      src={asset.thumbnailUrl ?? asset.url}
                      alt={asset.originalName}
                      fill
                      sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 20vw"
                      style={{ objectFit: 'cover' }}
                    />
                  ) : (
                    <FileIcon mimeType={asset.mimeType} />
                  )}
                  {/* Hover actions */}
                  <div className="absolute inset-0 bg-black/60 flex items-center justify-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      title="Copiar URL"
                      onClick={() => copyUrl(asset.url)}
                      className="w-7 h-7 bg-accent/90 hover:bg-accent text-white flex items-center justify-center rounded"
                    >
                      <IconSet.Copy size={12} />
                    </button>
                    <button
                      title="Eliminar"
                      onClick={() => {
                        if (confirm('Eliminar aquest fitxer?')) doDelete(asset.id);
                      }}
                      className="w-7 h-7 bg-danger/90 hover:bg-danger text-white flex items-center justify-center rounded"
                    >
                      <IconSet.Trash size={12} />
                    </button>
                  </div>
                </div>
                {/* Info */}
                <div className="p-2">
                  <div className="f-mono text-label text-ink-0 truncate text-[10px]">{asset.originalName}</div>
                  <div className="f-mono text-label text-ink-3 text-[9px] flex items-center justify-between mt-0.5">
                    <span>{fmtSize(asset.size)}</span>
                    <span>{fmtDate(asset.createdAt)}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </PortalShell>
  );
}
