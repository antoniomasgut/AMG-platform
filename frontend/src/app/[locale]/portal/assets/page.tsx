'use client';

import { useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { listAssets, uploadAsset, deleteAsset, type AssetResponse } from '@/services/assets';
import { PortalShell } from '@/components/portal/PortalShell';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';

function fmtSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function fmtDate(d: string) {
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function AssetsPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const qc = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const tenantId = user?.tenantId ?? '';

  const { data: assets = [], isLoading } = useQuery({
    queryKey: ['assets', tenantId],
    queryFn: () => listAssets(tenantId),
    enabled: !!tenantId,
  });

  const { mutate: doUpload, isPending: uploading } = useMutation({
    mutationFn: (file: File) => uploadAsset(tenantId, file),
    onSuccess: () => {
      toast('success', 'Fitxer pujat correctament');
      qc.invalidateQueries({ queryKey: ['assets', tenantId] });
    },
    onError: () => toast('error', "Error pujant el fitxer"),
  });

  const { mutate: doDelete } = useMutation({
    mutationFn: (assetId: string) => deleteAsset(assetId),
    onSuccess: () => {
      toast('success', 'Fitxer eliminat');
      qc.invalidateQueries({ queryKey: ['assets', tenantId] });
    },
    onError: () => toast('error', "Error eliminant el fitxer"),
  });

  const handleFiles = (files: FileList | null) => {
    if (!files) return;
    Array.from(files).forEach((file) => doUpload(file));
  };

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
          <AMGButton icon={I.Upload} loading={uploading} onClick={() => inputRef.current?.click()}>
            Pujar fitxer
          </AMGButton>
          <input
            ref={inputRef}
            type="file"
            multiple
            className="hidden"
            onChange={(e) => handleFiles(e.target.files)}
          />
        </div>

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
              <I.Upload size={28} stroke="#64748b" className="mx-auto mb-3" />
              <div className="f-display font-bold text-sm mb-1">Arrossega fitxers aquí</div>
              <p className="f-mono text-label text-ink-2">o fes clic a "Pujar fitxer"</p>
            </>
          )}
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          </div>
        ) : assets.length === 0 ? (
          <div className="amg-card card-clip p-8 text-center">
            <I.Image size={28} stroke="#64748b" className="mx-auto mb-3" />
            <div className="f-display font-bold text-sm mb-1">Cap fitxer</div>
            <p className="f-mono text-label text-ink-2">Puja el primer fitxer del teu tenant</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
            {(assets as AssetResponse[]).map((asset) => (
              <div key={asset.id} className="amg-card card-clip overflow-hidden group">
                {/* Miniatura */}
                <div className="aspect-square bg-bg-1 relative flex items-center justify-center">
                  {isImage(asset.mimeType) ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={asset.thumbnailUrl ?? asset.url}
                      alt={asset.originalName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <I.Box size={32} stroke="#64748b" />
                  )}
                  <button
                    onClick={() => {
                      if (confirm('Eliminar aquest fitxer?')) doDelete(asset.id);
                    }}
                    className="absolute top-2 right-2 w-7 h-7 bg-danger/80 hover:bg-danger text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <I.Trash size={12} />
                  </button>
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
